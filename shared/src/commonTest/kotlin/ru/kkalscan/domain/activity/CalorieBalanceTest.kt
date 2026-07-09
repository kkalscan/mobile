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
            activity = ResolvedActivity(ActivitySource.Emulator, 420, 10_500),
        )
        balance.eatenKcal shouldBe 1840
        balance.burnedKcal shouldBe 420
        balance.deficitKcal shouldBe -1420
        balance.isSurplus shouldBe true
        balance.isDeficit shouldBe false
    }

    @Test
    fun deficit_includesWorkoutsAndActivity() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-04",
                totalKcal = 1200,
                totalBurnedKcal = 280,
            ),
            activity = ResolvedActivity(ActivitySource.DeviceSensor, 350, 8750),
        )
        balance.burnedKcal shouldBe 630
        balance.workoutKcal shouldBe 280
        balance.activityKcal shouldBe 350
        balance.activitySource shouldBe ActivitySource.DeviceSensor
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
            activity = ResolvedActivity(ActivitySource.Emulator, 500, 12_500),
        )
        balance.deficitKcal shouldBe 120
        balance.isDeficit shouldBe true
    }
}
