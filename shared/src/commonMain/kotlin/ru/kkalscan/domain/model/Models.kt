package ru.kkalscan.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MealType {
    breakfast,
    lunch,
    dinner,
    snack,
}

@Serializable
data class Dish(
    val name: String,
    val grams: Int,
    val kcal: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double = 0.0,
)

@Serializable
data class DiaryEntry(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("meal_type") val mealType: MealType,
    @SerialName("total_kcal") val totalKcal: Int,
    val dishes: List<Dish>,
)

@Serializable
data class DiaryDay(
    val date: String,
    @SerialName("total_kcal") val totalKcal: Int,
    @SerialName("scans_left") val scansLeft: Int? = null,
    @SerialName("is_pro") val isPro: Boolean = false,
    @SerialName("account_linked") val accountLinked: Boolean = false,
    @SerialName("linked_providers") val linkedProviders: List<String> = emptyList(),
    val entries: List<DiaryEntry> = emptyList(),
)

@Serializable
data class ScanResult(
    @SerialName("scan_id") val scanId: String,
    val dishes: List<Dish>,
    @SerialName("total_kcal") val totalKcal: Int,
    @SerialName("total_protein") val totalProtein: Double,
    @SerialName("total_fat") val totalFat: Double,
    @SerialName("total_carbs") val totalCarbs: Double,
    @SerialName("total_fiber") val totalFiber: Double = 0.0,
    @SerialName("scans_left") val scansLeft: Int? = null,
    @SerialName("is_pro") val isPro: Boolean = false,
    val disclaimer: String? = null,
)

@Serializable
data class SubscriptionStatus(
    @SerialName("is_pro") val isPro: Boolean,
    @SerialName("pro_until") val proUntil: String? = null,
    @SerialName("account_linked") val accountLinked: Boolean = false,
    @SerialName("linked_providers") val linkedProviders: List<String> = emptyList(),
    val tariff: String? = null,
)

@Serializable
data class ScanBonusResult(
    @SerialName("scans_left") val scansLeft: Int,
    @SerialName("bonus_granted") val bonusGranted: Boolean,
)

@Serializable
data class CreateDiaryEntryResponse(
    val entry: DiaryEntry,
    @SerialName("scans_left") val scansLeft: Int? = null,
)

@Serializable
data class ApiErrorBody(
    val error: String,
    val message: String,
    @SerialName("scans_left") val scansLeft: Int? = null,
)

@Serializable
data class BugReportResult(
    @SerialName("report_id") val reportId: String,
    @SerialName("is_pro") val isPro: Boolean,
    @SerialName("pro_until") val proUntil: String? = null,
    val message: String,
)

@Serializable
data class FoodSearchResult(
    val query: String,
    val items: List<Dish>,
    val total: Int,
)

@Serializable
data class WorkoutEntry(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    val name: String,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val kcal: Int,
    val description: String? = null,
)

@Serializable
data class WorkoutResult(
    @SerialName("workout_id") val workoutId: String,
    val name: String,
    val kcal: Int,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
)

@Serializable
data class ActivityDay(
    val date: String,
    @SerialName("health_connect_kcal") val healthConnectKcal: Int = 0,
    val steps: Int? = null,
    val workouts: List<WorkoutEntry> = emptyList(),
) {
    val workoutKcal: Int get() = workouts.sumOf { it.kcal }
    val totalBurnedKcal: Int get() = healthConnectKcal + workoutKcal
}

@Serializable
data class ProSubscriptionStart(
    @SerialName("is_pro") val isPro: Boolean,
    @SerialName("pro_until") val proUntil: String? = null,
    val tariff: String,
    @SerialName("payment_required") val paymentRequired: Boolean,
    @SerialName("payment_url") val paymentUrl: String? = null,
    @SerialName("payment_id") val paymentId: String? = null,
    val message: String? = null,
)
