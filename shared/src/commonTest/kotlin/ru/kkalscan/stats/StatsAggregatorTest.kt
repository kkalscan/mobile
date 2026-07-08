package ru.kkalscan.stats

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType

class StatsAggregatorTest {

    @Test
    fun dayMetrics_includesBurnedKcalFromDay() {
        val day = DiaryDay("2026-06-10", 0, totalBurnedKcal = 350, entries = emptyList())
        val metrics = StatsAggregator.dayMetrics(day)
        metrics.burnedKcal shouldBe 350
        metrics.hasData shouldBe true
    }

    @Test
    fun dayMetrics_hasDataFalseWhenNoEntriesAndNoBurned() {
        val day = DiaryDay("2026-06-10", 0, entries = emptyList())
        val metrics = StatsAggregator.dayMetrics(day)
        metrics.hasData shouldBe false
    }

    @Test
    fun weekStats_includesBurnedKcalTotals() {
        val dish = Dish("Рис", 200, 300, 8.0, 2.0, 60.0, fiber = 4.0)
        val entry = DiaryEntry("1", "2026-06-10T12:00:00Z", MealType.lunch, 300, listOf(dish))
        val days = listOf(
            DiaryDay("2026-06-09", 0, totalBurnedKcal = 200, entries = emptyList()),
            DiaryDay("2026-06-10", 300, totalBurnedKcal = 400, entries = listOf(entry)),
        )
        val stats = StatsAggregator.weekStats(days, "2026-06-09")
        stats.daysWithData shouldBe 2
        stats.totalBurnedKcal shouldBe 600
        stats.avgBurnedKcal shouldBe 300
    }

    @Test
    fun dayMetrics_includesFiberFromDishes() {
        val dish = Dish("Рис", 200, 300, 8.0, 2.0, 60.0, fiber = 4.0)
        val entry = DiaryEntry("1", "2026-06-10T12:00:00Z", MealType.lunch, 300, listOf(dish))
        val day = DiaryDay("2026-06-10", 300, entries = listOf(entry))
        val metrics = StatsAggregator.dayMetrics(day)
        metrics.fiber shouldBe 4.0
    }

    @Test
    fun weekStats_averagesOnlyDaysWithEntries() {
        val dish = Dish("Рис", 200, 300, 8.0, 2.0, 60.0, fiber = 4.0)
        val entry = DiaryEntry("1", "2026-06-10T12:00:00Z", MealType.lunch, 300, listOf(dish))
        val days = listOf(
            DiaryDay("2026-06-09", 0, entries = emptyList()),
            DiaryDay("2026-06-10", 300, entries = listOf(entry)),
            DiaryDay("2026-06-11", 600, entries = listOf(entry, entry.copy(id = "2"))),
        )
        val stats = StatsAggregator.weekStats(days, "2026-06-09")
        stats.daysWithData shouldBe 2
        stats.avgKcal shouldBe 450
        stats.totalKcal shouldBe 900
        stats.avgProtein shouldBe 12.0
        stats.avgFiber shouldBe 6.0
    }

    @Test
    fun macroKcalSplit_usesStandardFactors() {
        val split = StatsAggregator.macroKcalSplit(protein = 100.0, fat = 50.0, carbs = 200.0)
        split.proteinKcal shouldBe 400.0
        split.fatKcal shouldBe 450.0
        split.carbsKcal shouldBe 800.0
        split.proteinPercent + split.fatPercent + split.carbsPercent in 99..101
    }
}
