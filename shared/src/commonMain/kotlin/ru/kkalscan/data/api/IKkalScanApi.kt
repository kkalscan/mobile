package ru.kkalscan.data.api

import ru.kkalscan.domain.model.ActivityEmulator
import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.FeatureSearchIntentResult
import ru.kkalscan.domain.model.FeatureSearchResult
import ru.kkalscan.domain.model.CreateDiaryEntryResponse
import ru.kkalscan.domain.model.CreateWorkoutResponse
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ProSubscriptionStart
import ru.kkalscan.domain.model.PromoApplyResult
import ru.kkalscan.domain.model.ScanBonusResult
import ru.kkalscan.domain.model.ScanResult
import ru.kkalscan.domain.model.SubscriptionOffers
import ru.kkalscan.domain.model.SubscriptionStatus
import ru.kkalscan.domain.model.WorkoutParseResult
import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.activity.wireName
import ru.kkalscan.insights.DietitianInsight

interface IKkalScanApi {
    suspend fun scanPhoto(deviceId: String, photoBytes: ByteArray, timezoneOffsetMinutes: Int): ScanResult
    suspend fun describeFood(deviceId: String, description: String, timezoneOffsetMinutes: Int): ScanResult
    suspend fun parseWorkout(deviceId: String, description: String): WorkoutParseResult
    suspend fun grantScanBonus(deviceId: String): ScanBonusResult
    suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay
    suspend fun getActivityEmulator(deviceId: String, timezoneOffsetMinutes: Int): ActivityEmulator
    suspend fun addDiaryEntry(
        deviceId: String,
        mealType: MealType,
        scanId: String? = null,
        dishes: List<ru.kkalscan.domain.model.Dish>? = null,
    ): CreateDiaryEntryResponse
    suspend fun deleteDiaryEntry(deviceId: String, entryId: String)
    suspend fun addWorkout(deviceId: String, name: String, kcal: Int): CreateWorkoutResponse
    suspend fun deleteWorkout(deviceId: String, workoutId: String)
    suspend fun syncActivity(
        deviceId: String,
        steps: Int,
        kcal: Int,
        source: ActivitySource,
        timezoneOffsetMinutes: Int,
    ): DiaryDay
    suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus
    suspend fun getSubscriptionOffers(deviceId: String): SubscriptionOffers
    suspend fun applyPromo(deviceId: String, promoCode: String): PromoApplyResult
    suspend fun startProSubscription(deviceId: String, tariff: String = "pro_monthly_199"): ProSubscriptionStart
    suspend fun submitBugReport(
        deviceId: String,
        email: String,
        description: String,
        screenshots: List<ByteArray>,
    ): BugReportResult
    suspend fun searchFeatures(
        deviceId: String,
        query: String,
        limit: Int = 20,
        locale: String = "ru",
    ): FeatureSearchResult

    suspend fun classifyFeatureSearchIntent(
        deviceId: String,
        query: String,
    ): FeatureSearchIntentResult

    suspend fun requestDietitianInsight(
        deviceId: String,
        weekStart: String,
        timezoneOffsetMinutes: Int,
    ): DietitianInsight
}
