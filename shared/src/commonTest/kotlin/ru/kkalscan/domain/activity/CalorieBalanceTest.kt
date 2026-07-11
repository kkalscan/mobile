package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.WorkoutEntry
import kotlin.test.Test

class CalorieBalanceTest {

    private val profile110 = EnergyProfile(weightKg = 110.0, heightCm = 180.0, ageYears = 40)

    @Test
    fun includesBmrAndWalking() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(date = "2026-07-11", totalKcal = 1800),
            liveActivity = ResolvedActivity(ActivitySource.DeviceSensor, 1143, 19_000),
            profile = profile110,
            dayFraction = 1.0,
        )
        balance.bmrKcal shouldBe 2030
        balance.restingKcal shouldBe 2030
        balance.activityKcal shouldBe 1143
        balance.burnedKcal shouldBe 3173
        balance.deficitKcal shouldBe 1373
    }

    @Test
    fun deficit_includesWorkoutsAndActivity() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-04",
                totalKcal = 1200,
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
            profile = EnergyProfile(weightKg = 70.0),
            dayFraction = 1.0,
        )
        balance.workoutKcal shouldBe 280
        balance.activityKcal shouldBe 335
        balance.burnedKcal shouldBe balance.restingKcal + 335 + 280
    }

    @Test
    fun recalculatesStepsFromProfile_notStaleServerKcal() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-04",
                totalKcal = 452,
                totalBurnedKcal = 1102,
                activityKcal = 756,
                activitySteps = 19_000,
                activitySource = "device_sensor",
            ),
            liveActivity = ResolvedActivity(ActivitySource.DeviceSensor, 900, 22_000),
            profile = profile110,
            dayFraction = 1.0,
        )
        balance.activityKcal shouldBe 1143
        balance.burnedKcal shouldBe 2030 + 1143
    }

    @Test
    fun liveActivity_fillsMissingServerStepsWhenWorkoutPersisted() {
        val balance = CalorieBalanceCalculator.compute(
            day = DiaryDay(
                date = "2026-07-10",
                totalKcal = 452,
                workouts = listOf(
                    WorkoutEntry(
                        id = "w1",
                        createdAt = "2026-07-10T12:00:00Z",
                        name = "Бег",
                        kcal = 280,
                    ),
                ),
            ),
            liveActivity = ResolvedActivity(ActivitySource.DeviceSensor, 1143, 19_000),
            profile = profile110,
            dayFraction = 0.5,
        )
        balance.activityKcal shouldBe 1143
        balance.workoutKcal shouldBe 280
        balance.restingKcal shouldBe 1015
        balance.burnedKcal shouldBe 1015 + 1143 + 280
    }
}
