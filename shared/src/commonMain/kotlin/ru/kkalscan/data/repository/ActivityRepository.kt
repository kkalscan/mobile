package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.health.IHealthConnectReader
import ru.kkalscan.data.repository.currentDateIso
import ru.kkalscan.data.repository.currentTimezoneOffsetMinutes
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.data.storage.IWorkoutStorage
import ru.kkalscan.domain.model.ActivityDay
import ru.kkalscan.domain.model.WorkoutEntry
import ru.kkalscan.domain.model.WorkoutResult

interface IActivityRepository {
    fun currentDate(): String
    suspend fun getToday(timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): ActivityDay
    suspend fun getDay(date: String, timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): ActivityDay
    suspend fun describeWorkout(description: String, timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): WorkoutResult
    suspend fun addWorkout(result: WorkoutResult, description: String? = null): ActivityDay
    suspend fun deleteWorkout(entryId: String)
}

class ActivityRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
    private val healthConnect: IHealthConnectReader,
    private val workoutStorage: IWorkoutStorage,
    private val todayProvider: () -> String = { currentDateIso() },
) : IActivityRepository {

    override fun currentDate(): String = todayProvider()

    override suspend fun getToday(timezoneOffsetMinutes: Int): ActivityDay =
        getDay(todayProvider(), timezoneOffsetMinutes)

    override suspend fun getDay(date: String, timezoneOffsetMinutes: Int): ActivityDay {
        val deviceId = deviceIdStorage.getDeviceId()
        val workouts = workoutStorage.getWorkouts(deviceId, date)
        val healthConnectKcal = if (date == todayProvider()) {
            healthConnect.readTodayActiveCalories()
        } else {
            0
        }
        val steps = if (date == todayProvider()) {
            healthConnect.readTodaySteps()
        } else {
            null
        }
        return ActivityDay(
            date = date,
            healthConnectKcal = healthConnectKcal,
            steps = steps,
            workouts = workouts,
        )
    }

    override suspend fun describeWorkout(description: String, timezoneOffsetMinutes: Int): WorkoutResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.describeWorkout(deviceId, description, timezoneOffsetMinutes)
    }

    override suspend fun addWorkout(result: WorkoutResult, description: String?): ActivityDay {
        val deviceId = deviceIdStorage.getDeviceId()
        val date = todayProvider()
        val entry = WorkoutEntry(
            id = result.workoutId,
            createdAt = "${date}T12:00:00Z",
            name = result.name,
            durationMinutes = result.durationMinutes,
            kcal = result.kcal,
            description = description,
        )
        workoutStorage.addWorkout(deviceId, date, entry)
        return getToday()
    }

    override suspend fun deleteWorkout(entryId: String) {
        val deviceId = deviceIdStorage.getDeviceId()
        workoutStorage.deleteWorkout(deviceId, entryId)
    }
}
