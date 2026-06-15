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
