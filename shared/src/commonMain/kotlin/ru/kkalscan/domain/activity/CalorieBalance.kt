package ru.kkalscan.domain.activity

import ru.kkalscan.domain.model.DiaryDay

data class CalorieBalance(
    val eatenKcal: Int,
    val burnedKcal: Int,
    val restingKcal: Int,
    val bmrKcal: Int,
    val activityKcal: Int,
    val activitySource: ActivitySource,
    val workoutKcal: Int,
    /** Потрачено минус съедено. Положительное значение — дефицит. */
    val deficitKcal: Int,
    val steps: Int?,
) {
    val isDeficit: Boolean get() = deficitKcal > 0
    val isSurplus: Boolean get() = deficitKcal < 0
}

object CalorieBalanceCalculator {
    fun compute(
        day: DiaryDay,
        liveActivity: ResolvedActivity = ResolvedActivity(ActivitySource.None, 0, null),
        profile: EnergyProfile = EnergyProfile(),
        dayFraction: Double = 1.0,
    ): CalorieBalance {
        val p = profile.normalized()
        val eatenKcal = day.totalKcal
        val workoutKcal = day.workouts.sumOf { it.kcal }
        val bmrKcal = BmrCalculator.dailyBmr(p)
        val restingKcal = BmrCalculator.proratedBmr(p, dayFraction)

        val persistedSteps = day.activitySteps?.takeIf { it > 0 }
        val steps = persistedSteps ?: if (liveActivity.source == ActivitySource.DeviceSensor) liveActivity.steps else null
        val activityKcal = when {
            persistedSteps != null -> StepCalorieEstimator.estimate(persistedSteps, p)
            liveActivity.activeKcal > 0 -> liveActivity.activeKcal
            day.activityKcal > 0 -> day.activityKcal
            else -> 0
        }
        val activitySource = when {
            persistedSteps != null ->
                if (liveActivity.source == ActivitySource.DeviceSensor) liveActivity.source
                else activitySourceFromWire(day.activitySource).takeIf { it != ActivitySource.None }
                    ?: ActivitySource.DeviceSensor
            liveActivity.activeKcal > 0 -> liveActivity.source
            day.activityKcal > 0 -> activitySourceFromWire(day.activitySource)
            else -> ActivitySource.None
        }

        val burnedKcal = restingKcal + activityKcal + workoutKcal
        return CalorieBalance(
            eatenKcal = eatenKcal,
            burnedKcal = burnedKcal,
            restingKcal = restingKcal,
            bmrKcal = bmrKcal,
            activityKcal = activityKcal,
            activitySource = activitySource,
            workoutKcal = workoutKcal,
            deficitKcal = burnedKcal - eatenKcal,
            steps = steps,
        )
    }
}
