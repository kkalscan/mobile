package ru.kkalscan.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.local.DiaryResource
import ru.kkalscan.data.local.IDiaryLocalStore
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.WorkoutParseResult
import ru.kkalscan.stats.WeekDates
import ru.kkalscan.util.kkalLog
import ru.kkalscan.util.maskDeviceId

interface IDiaryRepository {
    /** Current calendar date in ISO (yyyy-MM-dd), re-read from the clock on every call. */
    fun currentDate(): String
    fun observeDay(
        date: String,
        timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes(),
    ): Flow<DiaryResource>
    fun observeToday(timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): Flow<DiaryResource>
    suspend fun getToday(timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): DiaryDay
    suspend fun getDay(date: String, timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): DiaryDay
    suspend fun getWeek(weekStartIso: String, timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): List<DiaryDay>
    suspend fun addFromScan(scanId: String, mealType: MealType, dishes: List<Dish>): DiaryDay
    suspend fun addFromDishes(dishes: List<Dish>, mealType: MealType): DiaryDay
    suspend fun addWorkout(name: String, kcal: Int): DiaryDay
    suspend fun syncActivity(steps: Int, kcal: Int, source: ActivitySource): DiaryDay
    suspend fun parseWorkout(description: String): WorkoutParseResult
    suspend fun deleteEntry(entryId: String)
    suspend fun deleteWorkout(workoutId: String)
}

class DiaryRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
    private val localStore: IDiaryLocalStore = InMemoryDiaryLocalStore(),
    private val todayProvider: () -> String = { currentDateIso() },
) : IDiaryRepository {

    override fun currentDate(): String = todayProvider()

    override fun observeToday(timezoneOffsetMinutes: Int): Flow<DiaryResource> =
        observeDay(todayProvider(), timezoneOffsetMinutes)

    override fun observeDay(date: String, timezoneOffsetMinutes: Int): Flow<DiaryResource> = flow {
        val refreshing = MutableStateFlow(true)
        val refreshError = MutableStateFlow<Throwable?>(null)
        coroutineScope {
            launch {
                runCatching { refreshDayFromNetwork(date, timezoneOffsetMinutes) }
                    .onSuccess {
                        refreshError.value = null
                        kkalLog("Diary", "observeDay refresh ok date=$date")
                    }
                    .onFailure { e ->
                        refreshError.value = e
                        kkalLog("Diary", "observeDay refresh fail date=$date ${e::class.simpleName}: ${e.message}")
                    }
                refreshing.value = false
            }
            combine(localStore.observeDay(date), refreshing, refreshError) { day, isRefreshing, error ->
                DiaryResource(
                    day = day,
                    isRefreshing = isRefreshing,
                    error = error.takeIf { !isRefreshing && day == null },
                )
            }.collect { emit(it) }
        }
    }

    override suspend fun getToday(timezoneOffsetMinutes: Int): DiaryDay =
        getDay(todayProvider(), timezoneOffsetMinutes)

    override suspend fun getDay(date: String, timezoneOffsetMinutes: Int): DiaryDay {
        runCatching { refreshDayFromNetwork(date, timezoneOffsetMinutes) }
            .onFailure { e ->
                localStore.getDay(date)?.let { return it }
                throw e
            }
        return localStore.getDay(date)
            ?: error("Diary day $date missing after refresh")
    }

    override suspend fun getWeek(weekStartIso: String, timezoneOffsetMinutes: Int): List<DiaryDay> =
        coroutineScope {
            WeekDates.weekFrom(WeekDates.parse(weekStartIso)).map { date ->
                async { getDay(date, timezoneOffsetMinutes) }
            }.map { it.await() }
        }

    override suspend fun addFromScan(scanId: String, mealType: MealType, dishes: List<Dish>): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        api.addDiaryEntry(deviceId, mealType, scanId, dishes)
        return refreshDayFromNetwork(todayProvider(), currentTimezoneOffsetMinutes())
    }

    override suspend fun addFromDishes(dishes: List<Dish>, mealType: MealType): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        api.addDiaryEntry(deviceId, mealType, scanId = null, dishes = dishes)
        return refreshDayFromNetwork(todayProvider(), currentTimezoneOffsetMinutes())
    }

    override suspend fun deleteEntry(entryId: String) {
        val deviceId = deviceIdStorage.getDeviceId()
        api.deleteDiaryEntry(deviceId, entryId)
        runCatching { refreshDayFromNetwork(todayProvider(), currentTimezoneOffsetMinutes()) }
    }

    override suspend fun addWorkout(name: String, kcal: Int): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        api.addWorkout(deviceId, name, kcal)
        return refreshDayFromNetwork(todayProvider(), currentTimezoneOffsetMinutes())
    }

    override suspend fun syncActivity(steps: Int, kcal: Int, source: ActivitySource): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        val date = todayProvider()
        val day = api.syncActivity(deviceId, steps, kcal, source, currentTimezoneOffsetMinutes())
        localStore.upsert(day)
        kkalLog(
            "Diary",
            "syncActivity device=${maskDeviceId(deviceId)} date=$date steps=$steps kcal=$kcal source=$source",
        )
        return day
    }

    override suspend fun parseWorkout(description: String): WorkoutParseResult {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.parseWorkout(deviceId, description)
    }

    override suspend fun deleteWorkout(workoutId: String) {
        val deviceId = deviceIdStorage.getDeviceId()
        api.deleteWorkout(deviceId, workoutId)
        runCatching { refreshDayFromNetwork(todayProvider(), currentTimezoneOffsetMinutes()) }
    }

    private suspend fun refreshDayFromNetwork(date: String, timezoneOffsetMinutes: Int): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        val day = api.getDiary(deviceId, date, timezoneOffsetMinutes)
        localStore.upsert(day)
        kkalLog(
            "Diary",
            "getDay device=${maskDeviceId(deviceId)} date=$date entries=${day.entries.size} kcal=${day.totalKcal}",
        )
        return day
    }
}

expect fun currentDateIso(): String
