package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import ru.kkalscan.domain.model.ActivityDay
import kotlin.test.Test

class CalorieBalanceTest {

    @Test
    fun deficit_isBurnedMinusEaten() {
        val balance = CalorieBalanceCalculator.compute(
            eatenKcal = 1840,
            activity = ActivityDay(
                date = "2026-07-04",
                healthConnectKcal = 420,
                workouts = emptyList(),
            ),
        )
        balance.eatenKcal shouldBe 1840
        balance.burnedKcal shouldBe 420
        balance.deficitKcal shouldBe -1420
        balance.isSurplus shouldBe true
        balance.isDeficit shouldBe false
    }

    @Test
    fun deficit_includesWorkoutsAndHealthConnect() {
        val balance = CalorieBalanceCalculator.compute(
            eatenKcal = 1200,
            activity = ActivityDay(
                date = "2026-07-04",
                healthConnectKcal = 350,
                workouts = listOf(
                    ru.kkalscan.domain.model.WorkoutEntry(
                        id = "w1",
                        createdAt = "2026-07-04T10:00:00Z",
                        name = "Бег",
                        durationMinutes = 30,
                        kcal = 280,
                    ),
                ),
            ),
        )
        balance.burnedKcal shouldBe 630
        balance.workoutKcal shouldBe 280
        balance.healthConnectKcal shouldBe 350
        balance.deficitKcal shouldBe -570
    }

    @Test
    fun positiveDeficit_whenBurnedExceedsEaten() {
        val balance = CalorieBalanceCalculator.compute(
            eatenKcal = 800,
            activity = ActivityDay(
                date = "2026-07-04",
                healthConnectKcal = 500,
                workouts = listOf(
                    ru.kkalscan.domain.model.WorkoutEntry(
                        id = "w1",
                        createdAt = "2026-07-04T10:00:00Z",
                        name = "Силовая",
                        durationMinutes = 60,
                        kcal = 420,
                    ),
                ),
            ),
        )
        balance.deficitKcal shouldBe 120
        balance.isDeficit shouldBe true
    }
}
