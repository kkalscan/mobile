package ru.kkalscan.domain.activity

import ru.kkalscan.domain.model.DiaryDay

data class CalorieBalance(
    val eatenKcal: Int,
    val burnedKcal: Int,
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
    fun compute(day: DiaryDay, liveActivity: ResolvedActivity = ResolvedActivity(ActivitySource.None, 0, null)): CalorieBalance {
        val eatenKcal = day.totalKcal
        val workoutKcal = day.workouts.sumOf { it.kcal }
        val hasPersistedBurn = day.totalBurnedKcal > 0 ||
            day.activityKcal > 0 ||
            day.activitySteps != null ||
            !day.activitySource.isNullOrBlank() ||
            workoutKcal > 0

        if (hasPersistedBurn) {
            val activityKcal = when {
                day.activityKcal > 0 -> day.activityKcal
                liveActivity.source == ActivitySource.DeviceSensor && liveActivity.activeKcal > 0 -> liveActivity.activeKcal
                else -> 0
            }
            val steps = day.activitySteps ?: if (liveActivity.source == ActivitySource.DeviceSensor) liveActivity.steps else null
            val burnedKcal = maxOf(day.totalBurnedKcal, workoutKcal + activityKcal)
            val activitySource = when {
                day.activityKcal > 0 -> activitySourceFromWire(day.activitySource)
                liveActivity.source == ActivitySource.DeviceSensor && liveActivity.activeKcal > 0 -> liveActivity.source
                else -> activitySourceFromWire(day.activitySource)
            }
            return CalorieBalance(
                eatenKcal = eatenKcal,
                burnedKcal = burnedKcal,
                activityKcal = activityKcal,
                activitySource = activitySource,
                workoutKcal = workoutKcal,
                deficitKcal = burnedKcal - eatenKcal,
                steps = steps,
            )
        }

        val activityKcal = liveActivity.activeKcal
        val burnedKcal = workoutKcal + activityKcal
        return CalorieBalance(
            eatenKcal = eatenKcal,
            burnedKcal = burnedKcal,
            activityKcal = activityKcal,
            activitySource = liveActivity.source,
            workoutKcal = workoutKcal,
            deficitKcal = burnedKcal - eatenKcal,
            steps = liveActivity.steps,
        )
    }
}
