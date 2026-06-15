package ru.kkalscan.data.api

import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.CreateDiaryEntryResponse
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ScanBonusResult
import ru.kkalscan.domain.model.ScanResult
import ru.kkalscan.domain.model.SubscriptionStatus

interface IKkalScanApi {
    suspend fun scanPhoto(deviceId: String, photoBytes: ByteArray, timezoneOffsetMinutes: Int): ScanResult
    suspend fun grantScanBonus(deviceId: String): ScanBonusResult
    suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay
    suspend fun addDiaryEntry(
        deviceId: String,
        mealType: MealType,
        scanId: String? = null,
        dishes: List<ru.kkalscan.domain.model.Dish>? = null,
    ): CreateDiaryEntryResponse
    suspend fun deleteDiaryEntry(deviceId: String, entryId: String)
    suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus
    suspend fun submitBugReport(
        deviceId: String,
        email: String,
        description: String,
        screenshots: List<ByteArray>,
    ): BugReportResult
}
