package ru.kkalscan.app.ui.journal

import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.stats.StatsAggregator
import ru.kkalscan.stats.WeekDates
import ru.kkalscan.stats.WeekStats

object JournalPreviewData {
    fun sampleWeekStats(weekStart: String = WeekDates.currentWeekStart()): WeekStats {
        val kcals = listOf(0, 1420, 1680, 1200, 1950, 2100, 890)
        val days = WeekDates.weekFrom(WeekDates.parse(weekStart)).mapIndexed { index, date ->
            val kcal = kcals.getOrElse(index) { 0 }
            if (kcal == 0) {
                DiaryDay(date = date, totalKcal = 0)
            } else {
                val dish = Dish(
                    name = "Блюдо",
                    grams = 250,
                    kcal = kcal,
                    protein = kcal * 0.08,
                    fat = kcal * 0.03,
                    carbs = kcal * 0.12,
                )
                DiaryDay(
                    date = date,
                    totalKcal = kcal,
                    isPro = false,
                    entries = listOf(
                        DiaryEntry(
                            id = "e$index",
                            createdAt = "${date}T12:00:00Z",
                            mealType = MealType.lunch,
                            totalKcal = kcal,
                            dishes = listOf(dish),
                        ),
                    ),
                )
            }
        }
        return StatsAggregator.weekStats(days, weekStart)
    }
}
