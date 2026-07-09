package ru.kkalscan.domain.activity

object StepCalorieEstimator {
    /** Rough average (~0.04 kcal/step); close to My Health on Tecno (56 steps → ~2 kcal). */
    const val KCAL_PER_STEP = 0.04

    fun estimate(steps: Int): Int =
        (steps * KCAL_PER_STEP).toInt().coerceAtLeast(0)
}
