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
    fun compute(day: DiaryDay, activity: ResolvedActivity): CalorieBalance {
        val eatenKcal = day.totalKcal
        val workoutKcal = day.totalBurnedKcal
        val activityKcal = activity.activeKcal
        val burnedKcal = workoutKcal + activityKcal
        return CalorieBalance(
            eatenKcal = eatenKcal,
            burnedKcal = burnedKcal,
            activityKcal = activityKcal,
            activitySource = activity.source,
            workoutKcal = workoutKcal,
            deficitKcal = burnedKcal - eatenKcal,
            steps = activity.steps,
        )
    }
}
