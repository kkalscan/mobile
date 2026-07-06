package ru.kkalscan.domain.activity

import ru.kkalscan.domain.model.ActivityDay

data class CalorieBalance(
    val eatenKcal: Int,
    val burnedKcal: Int,
    val healthConnectKcal: Int,
    val workoutKcal: Int,
    /** Потрачено минус съедено. Положительное значение — дефицит. */
    val deficitKcal: Int,
) {
    val isDeficit: Boolean get() = deficitKcal > 0
    val isSurplus: Boolean get() = deficitKcal < 0
}

object CalorieBalanceCalculator {
    fun compute(eatenKcal: Int, activity: ActivityDay): CalorieBalance {
        val burned = activity.totalBurnedKcal
        return CalorieBalance(
            eatenKcal = eatenKcal,
            burnedKcal = burned,
            healthConnectKcal = activity.healthConnectKcal,
            workoutKcal = activity.workoutKcal,
            deficitKcal = burned - eatenKcal,
        )
    }
}
