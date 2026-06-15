package ru.kkalscan.insights

import kotlinx.serialization.Serializable

@Serializable
data class InsightSection(
    val title: String,
    val body: String,
)

@Serializable
data class DietitianInsight(
    @kotlinx.serialization.SerialName("week_start") val weekStart: String,
    @kotlinx.serialization.SerialName("generated_at") val generatedAt: String,
    val headline: String,
    val sections: List<InsightSection>,
    val disclaimer: String = "Не является медицинской рекомендацией. Обратитесь к врачу при необходимости.",
)
