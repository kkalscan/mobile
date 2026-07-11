package ru.kkalscan.domain.activity

object StepCalorieEstimator {
    /** Active kcal per step (~0.055); slightly above My Health baseline for walking. */
    const val KCAL_PER_STEP = 0.055

    fun estimate(steps: Int): Int =
        (steps * KCAL_PER_STEP).toInt().coerceAtLeast(0)
}
