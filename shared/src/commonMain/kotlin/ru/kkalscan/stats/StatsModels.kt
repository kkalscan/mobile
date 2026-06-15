package ru.kkalscan.stats

import ru.kkalscan.domain.model.DiaryDay

enum class MetricKind {
    Calories,
    Protein,
    Fat,
    Carbs,
}

data class DayMetrics(
    val date: String,
    val kcal: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val entryCount: Int,
) {
    val hasData: Boolean get() = entryCount > 0
}

data class WeekStats(
    val weekStart: String,
    val days: List<DayMetrics>,
    val isPro: Boolean = false,
    val avgKcal: Int = 0,
    val totalKcal: Int = 0,
    val avgProtein: Double = 0.0,
    val avgFat: Double = 0.0,
    val avgCarbs: Double = 0.0,
    val daysWithData: Int = 0,
)

object StatsAggregator {
    fun dayMetrics(day: DiaryDay): DayMetrics {
        val dishes = day.entries.flatMap { it.dishes }
        return DayMetrics(
            date = day.date,
            kcal = day.totalKcal,
            protein = dishes.sumOf { it.protein },
            fat = dishes.sumOf { it.fat },
            carbs = dishes.sumOf { it.carbs },
            entryCount = day.entries.size,
        )
    }

    fun weekStats(days: List<DiaryDay>, weekStart: String): WeekStats {
        val metrics = days.map { dayMetrics(it) }
        val withData = metrics.filter { it.hasData }
        val count = withData.size.coerceAtLeast(1)
        return WeekStats(
            weekStart = weekStart,
            days = metrics,
            isPro = days.any { it.isPro },
            avgKcal = if (withData.isEmpty()) 0 else withData.sumOf { it.kcal } / withData.size,
            totalKcal = metrics.sumOf { it.kcal },
            avgProtein = if (withData.isEmpty()) 0.0 else withData.sumOf { it.protein } / count,
            avgFat = if (withData.isEmpty()) 0.0 else withData.sumOf { it.fat } / count,
            avgCarbs = if (withData.isEmpty()) 0.0 else withData.sumOf { it.carbs } / count,
            daysWithData = withData.size,
        )
    }
}
