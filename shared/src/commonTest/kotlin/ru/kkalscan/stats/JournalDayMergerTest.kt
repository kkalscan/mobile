package ru.kkalscan.stats

import io.kotest.matchers.shouldBe
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.WorkoutEntry
import kotlin.test.Test

class JournalDayMergerTest {

    @Test
    fun replacesTodayInWeekWithPatch() {
        val today = "2026-07-10"
        val week = listOf(
            DiaryDay(date = "2026-07-07", totalKcal = 1000, totalBurnedKcal = 400),
            DiaryDay(date = today, totalKcal = 452, totalBurnedKcal = 0),
        )
        val patch = DiaryDay(
            date = today,
            totalKcal = 452,
            totalBurnedKcal = 1102,
            activityKcal = 822,
            activitySteps = 20_556,
            activitySource = "device_sensor",
            workouts = listOf(
                WorkoutEntry(
                    id = "w1",
                    createdAt = "2026-07-10T12:00:00Z",
                    name = "Бег",
                    kcal = 280,
                ),
            ),
        )
        val merged = JournalDayMerger.mergeWeekWithTodayPatch(week, patch)
        merged.single { it.date == today }.totalBurnedKcal shouldBe 1102
        merged.single { it.date == "2026-07-07" }.totalBurnedKcal shouldBe 400
    }

    @Test
    fun appendsTodayWhenMissingFromWeek() {
        val patch = DiaryDay(date = "2026-07-10", totalKcal = 0, totalBurnedKcal = 1500, activityKcal = 1500)
        val merged = JournalDayMerger.mergeWeekWithTodayPatch(emptyList(), patch)
        merged.size shouldBe 1
        merged.first().totalBurnedKcal shouldBe 1500
    }
}
