package ru.kkalscan.domain.activity

import ru.kkalscan.domain.model.DiaryDay

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
    fun compute(day: DiaryDay, healthConnectKcal: Int): CalorieBalance {
        val eatenKcal = day.totalKcal
        val workoutKcal = day.totalBurnedKcal
        val burnedKcal = workoutKcal + healthConnectKcal
        return CalorieBalance(
            eatenKcal = eatenKcal,
            burnedKcal = burnedKcal,
            healthConnectKcal = healthConnectKcal,
            workoutKcal = workoutKcal,
            deficitKcal = burnedKcal - eatenKcal,
        )
    }
}
