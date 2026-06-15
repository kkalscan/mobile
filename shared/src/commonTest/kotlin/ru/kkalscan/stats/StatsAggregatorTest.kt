package ru.kkalscan.stats

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType

class StatsAggregatorTest {

    @Test
    fun weekStats_averagesOnlyDaysWithEntries() {
        val dish = Dish("Рис", 200, 300, 8.0, 2.0, 60.0)
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
    }
}
