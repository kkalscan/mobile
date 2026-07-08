package ru.kkalscan.stats

import ru.kkalscan.domain.model.DiaryDay

enum class MetricKind {
    Calories,
    Protein,
    Fat,
    Carbs,
    Fiber,
}

data class DayMetrics(
    val date: String,
    val kcal: Int,
    val burnedKcal: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double,
    val entryCount: Int,
) {
    val hasData: Boolean get() = entryCount > 0 || burnedKcal > 0
}

data class WeekStats(
    val weekStart: String,
    val days: List<DayMetrics>,
    val isPro: Boolean = false,
    val avgKcal: Int = 0,
    val totalKcal: Int = 0,
    val avgBurnedKcal: Int = 0,
    val totalBurnedKcal: Int = 0,
    val avgProtein: Double = 0.0,
    val avgFat: Double = 0.0,
    val avgCarbs: Double = 0.0,
    val avgFiber: Double = 0.0,
    val daysWithData: Int = 0,
)

object StatsAggregator {
    fun dayMetrics(day: DiaryDay): DayMetrics {
        val dishes = day.entries.flatMap { it.dishes }
        return DayMetrics(
            date = day.date,
            kcal = day.totalKcal,
            burnedKcal = day.totalBurnedKcal,
            protein = dishes.sumOf { it.protein },
            fat = dishes.sumOf { it.fat },
            carbs = dishes.sumOf { it.carbs },
            fiber = dishes.sumOf { it.fiber },
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
            avgBurnedKcal = if (withData.isEmpty()) 0 else withData.sumOf { it.burnedKcal } / withData.size,
            totalBurnedKcal = metrics.sumOf { it.burnedKcal },
            avgProtein = if (withData.isEmpty()) 0.0 else withData.sumOf { it.protein } / count,
            avgFat = if (withData.isEmpty()) 0.0 else withData.sumOf { it.fat } / count,
            avgCarbs = if (withData.isEmpty()) 0.0 else withData.sumOf { it.carbs } / count,
            avgFiber = if (withData.isEmpty()) 0.0 else withData.sumOf { it.fiber } / count,
            daysWithData = withData.size,
        )
    }

    /** kcal from macros: protein/carbs × 4, fat × 9 */
    fun macroKcalSplit(protein: Double, fat: Double, carbs: Double): MacroKcalSplit {
        val proteinKcal = protein * 4
        val fatKcal = fat * 9
        val carbsKcal = carbs * 4
        val total = (proteinKcal + fatKcal + carbsKcal).coerceAtLeast(1.0)
        return MacroKcalSplit(
            proteinKcal = proteinKcal,
            fatKcal = fatKcal,
            carbsKcal = carbsKcal,
            totalKcal = total,
            proteinPercent = (proteinKcal / total * 100).toInt(),
            fatPercent = (fatKcal / total * 100).toInt(),
            carbsPercent = (carbsKcal / total * 100).toInt(),
        )
    }
}

data class MacroKcalSplit(
    val proteinKcal: Double,
    val fatKcal: Double,
    val carbsKcal: Double,
    val totalKcal: Double,
    val proteinPercent: Int,
    val fatPercent: Int,
    val carbsPercent: Int,
)
