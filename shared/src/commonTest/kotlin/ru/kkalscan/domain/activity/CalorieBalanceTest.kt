package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.WorkoutEntry
import kotlin.test.Test

class CalorieBalanceTest {

    @Test
    fun deficit_isBurnedMinusEaten() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-04",
                totalKcal = 1840,
            ),
            liveActivity = ResolvedActivity(ActivitySource.Emulator, 420, 10_500),
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
                totalBurnedKcal = 630,
                activityKcal = 350,
                activitySteps = 8750,
                activitySource = "device_sensor",
                workouts = listOf(
                    WorkoutEntry(
                        id = "w1",
                        createdAt = "2026-07-04T12:00:00Z",
                        name = "Бег",
                        kcal = 280,
                    ),
                ),
            ),
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
                totalBurnedKcal = 920,
                activityKcal = 500,
                activitySteps = 12_500,
                activitySource = "emulator",
                workouts = listOf(
                    WorkoutEntry(
                        id = "w1",
                        createdAt = "2026-07-04T12:00:00Z",
                        name = "Йога",
                        kcal = 420,
                    ),
                ),
            ),
        )
        balance.deficitKcal shouldBe 120
        balance.isDeficit shouldBe true
    }

    @Test
    fun serverBurn_winsOverHigherLiveActivity() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-04",
                totalKcal = 452,
                totalBurnedKcal = 1102,
                activityKcal = 822,
                activitySteps = 20_556,
                activitySource = "device_sensor",
                workouts = listOf(
                    WorkoutEntry(
                        id = "w1",
                        createdAt = "2026-07-04T12:00:00Z",
                        name = "Бег",
                        kcal = 280,
                    ),
                ),
            ),
            liveActivity = ResolvedActivity(ActivitySource.DeviceSensor, 900, 22_000),
        )
        balance.burnedKcal shouldBe 1102
        balance.activityKcal shouldBe 822
        balance.workoutKcal shouldBe 280
    }

    @Test
    fun persistedSteps_estimateKcalWhenActivityKcalMissing() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-10",
                totalKcal = 0,
                totalBurnedKcal = 0,
                activityKcal = 0,
                activitySteps = 37_500,
                activitySource = "device_sensor",
            ),
            liveActivity = ResolvedActivity(ActivitySource.Emulator, 0, null),
        )
        balance.burnedKcal shouldBe 2062
        balance.activityKcal shouldBe 2062
        balance.steps shouldBe 37_500
    }

    @Test
    fun liveActivity_fillsMissingServerStepsWhenWorkoutPersisted() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-10",
                totalKcal = 452,
                totalBurnedKcal = 280,
                activityKcal = 0,
                workouts = listOf(
                    WorkoutEntry(
                        id = "w1",
                        createdAt = "2026-07-10T12:00:00Z",
                        name = "Бег",
                        kcal = 280,
                    ),
                ),
            ),
            liveActivity = ResolvedActivity(ActivitySource.DeviceSensor, 1130, 20_556),
        )
        balance.burnedKcal shouldBe 1410
        balance.activityKcal shouldBe 1130
        balance.steps shouldBe 20_556
    }
}
