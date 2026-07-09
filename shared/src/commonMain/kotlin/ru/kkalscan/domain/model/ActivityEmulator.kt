package ru.kkalscan.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityEmulator(
    val mode: String,
    @SerialName("estimated_active_kcal") val estimatedActiveKcal: Int,
    @SerialName("estimated_steps") val estimatedSteps: Int,
    @SerialName("avg_consumed_kcal_per_day") val avgConsumedKcalPerDay: Int? = null,
    @SerialName("diary_days_with_entries") val diaryDaysWithEntries: Int = 0,
    @SerialName("lookback_days") val lookbackDays: Int = 30,
)
