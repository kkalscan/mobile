package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.MealType

interface IDiaryRepository {
    suspend fun getToday(timezoneOffsetMinutes: Int = 180): DiaryDay
    suspend fun addFromScan(scanId: String, mealType: MealType): DiaryDay
    suspend fun deleteEntry(entryId: String)
}

class DiaryRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
    private val todayProvider: () -> String = { currentDateIso() },
) : IDiaryRepository {

    override suspend fun getToday(timezoneOffsetMinutes: Int): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.getDiary(deviceId, todayProvider(), timezoneOffsetMinutes)
    }

    override suspend fun addFromScan(scanId: String, mealType: MealType): DiaryDay {
        val deviceId = deviceIdStorage.getDeviceId()
        api.addDiaryEntry(deviceId, mealType, scanId)
        return api.getDiary(deviceId, todayProvider())
    }

    override suspend fun deleteEntry(entryId: String) {
        val deviceId = deviceIdStorage.getDeviceId()
        api.deleteDiaryEntry(deviceId, entryId)
    }
}

expect fun currentDateIso(): String
