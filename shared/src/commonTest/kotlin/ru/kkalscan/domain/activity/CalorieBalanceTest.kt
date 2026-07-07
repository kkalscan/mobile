package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import ru.kkalscan.domain.model.DiaryDay
import kotlin.test.Test

class CalorieBalanceTest {

    @Test
    fun deficit_isBurnedMinusEaten() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-04",
                totalKcal = 1840,
            ),
            healthConnectKcal = 420,
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
            day = DiaryDay(
                date = "2026-07-04",
                totalKcal = 1200,
                totalBurnedKcal = 280,
            ),
            healthConnectKcal = 350,
        )
        balance.burnedKcal shouldBe 630
        balance.workoutKcal shouldBe 280
        balance.healthConnectKcal shouldBe 350
        balance.deficitKcal shouldBe -570
    }

    @Test
    fun positiveDeficit_whenBurnedExceedsEaten() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-04",
                totalKcal = 800,
                totalBurnedKcal = 420,
            ),
            healthConnectKcal = 500,
        )
        balance.deficitKcal shouldBe 120
        balance.isDeficit shouldBe true
    }
}
