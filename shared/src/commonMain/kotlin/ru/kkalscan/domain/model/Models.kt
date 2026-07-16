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
data class WorkoutEntry(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    val name: String,
    val kcal: Int,
)

@Serializable
data class DiaryDay(
    val date: String,
    @SerialName("total_kcal") val totalKcal: Int,
    @SerialName("total_burned_kcal") val totalBurnedKcal: Int = 0,
    @SerialName("net_kcal") val netKcal: Int = totalKcal - totalBurnedKcal,
    @SerialName("activity_kcal") val activityKcal: Int = 0,
    @SerialName("activity_steps") val activitySteps: Int? = null,
    @SerialName("activity_source") val activitySource: String? = null,
    @SerialName("scans_left") val scansLeft: Int? = null,
    @SerialName("is_pro") val isPro: Boolean = false,
    @SerialName("account_linked") val accountLinked: Boolean = false,
    @SerialName("linked_providers") val linkedProviders: List<String> = emptyList(),
    val entries: List<DiaryEntry> = emptyList(),
    val workouts: List<WorkoutEntry> = emptyList(),
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
data class CreateWorkoutResponse(
    val workout: WorkoutEntry,
)

@Serializable
data class WorkoutParseResult(
    val title: String,
    @SerialName("burned_kcal") val burnedKcal: Int,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
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
data class ProSubscriptionStart(
    @SerialName("is_pro") val isPro: Boolean,
    @SerialName("pro_until") val proUntil: String? = null,
    val tariff: String,
    @SerialName("payment_required") val paymentRequired: Boolean,
    @SerialName("payment_url") val paymentUrl: String? = null,
    @SerialName("payment_id") val paymentId: String? = null,
    val message: String? = null,
)

@Serializable
data class SubscriptionOffer(
    val tariff: String,
    val title: String,
    @SerialName("price_rub") val priceRub: Int,
    @SerialName("amount_rub") val amountRub: Int,
    @SerialName("amount_kopecks") val amountKopecks: Int,
    @SerialName("discount_percent") val discountPercent: Int = 0,
    @SerialName("promo_code") val promoCode: String? = null,
)

@Serializable
data class SubscriptionOffers(
    val offers: List<SubscriptionOffer>,
)

@Serializable
data class PromoApplyResult(
    @SerialName("promo_code") val promoCode: String,
    @SerialName("discount_percent") val discountPercent: Int,
)
