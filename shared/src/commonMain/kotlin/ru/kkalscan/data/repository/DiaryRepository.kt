package ru.kkalscan.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.stats.WeekDates
import ru.kkalscan.util.kkalLog
import ru.kkalscan.util.maskDeviceId

interface IDiaryRepository {
    suspend fun getToday(timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): DiaryDay
    suspend fun getDay(date: String, timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): DiaryDay
    suspend fun getWeek(weekStartIso: String, timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): List<DiaryDay>
    suspend fun addFromScan(scanId: String, mealType: MealType, dishes: List<ru.kkalscan.domain.model.Dish>): DiaryDay
    suspend fun deleteEntry(entryId: String)
}

class DiaryRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
    private val todayProvider: () -> String = { currentDateIso() },
) : IDiaryRepository {

    override suspend fun getToday(timezoneOffsetMinutes: Int): DiaryDay =
        getDay(todayProvider(), timezoneOffsetMinutes)

    override suspend fun getDay(date: String, timezoneOffsetMinutes: Int): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        val day = api.getDiary(deviceId, date, timezoneOffsetMinutes)
        kkalLog(
            "Diary",
            "getDay device=${maskDeviceId(deviceId)} date=$date entries=${day.entries.size} kcal=${day.totalKcal}",
        )
        return day
    }

    override suspend fun getWeek(weekStartIso: String, timezoneOffsetMinutes: Int): List<DiaryDay> =
        coroutineScope {
            WeekDates.weekFrom(WeekDates.parse(weekStartIso)).map { date ->
                async { getDay(date, timezoneOffsetMinutes) }
            }.map { it.await() }
        }

    override suspend fun addFromScan(scanId: String, mealType: MealType, dishes: List<ru.kkalscan.domain.model.Dish>): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        api.addDiaryEntry(deviceId, mealType, scanId, dishes)
        return api.getDiary(deviceId, todayProvider(), currentTimezoneOffsetMinutes())
    }

    override suspend fun deleteEntry(entryId: String) {
        val deviceId = deviceIdStorage.getDeviceId()
        api.deleteDiaryEntry(deviceId, entryId)
    }
}

expect fun currentDateIso(): String
