package ru.kkalscan.domain.activity

import kotlin.math.roundToInt

object StepCalorieEstimator {
    /** Walking MET (moderate pace). */
    const val WALKING_MET = 3.5
    const val STEP_LENGTH_M = 0.75
    const val WALKING_SPEED_KMH = 4.8

    /** Legacy alias for emulator step conversion at reference weight. */
    const val REFERENCE_WEIGHT_KG = EnergyProfile.DEFAULT_WEIGHT_KG
    const val DEFAULT_WEIGHT_KG = EnergyProfile.DEFAULT_WEIGHT_KG
    const val MIN_WEIGHT_KG = EnergyProfile.MIN_WEIGHT_KG.toInt()
    const val MAX_WEIGHT_KG = EnergyProfile.MAX_WEIGHT_KG.toInt()

    fun estimate(steps: Int, profile: EnergyProfile = EnergyProfile()): Int =
        estimate(steps, profile.normalized().weightKg)

    fun estimate(steps: Int, weightKg: Double): Int {
        if (steps <= 0) return 0
        val weight = weightKg.coerceIn(EnergyProfile.MIN_WEIGHT_KG, EnergyProfile.MAX_WEIGHT_KG)
        val distanceKm = steps * STEP_LENGTH_M / 1000.0
        val hours = distanceKm / WALKING_SPEED_KMH
        return (WALKING_MET * weight * hours).roundToInt().coerceAtLeast(0)
    }

    /** Approximate steps for a given active kcal (emulator / fake API). */
    fun stepsForKcal(kcal: Int, weightKg: Double = DEFAULT_WEIGHT_KG): Int {
        if (kcal <= 0) return 0
        val perStep = estimate(1, weightKg).coerceAtLeast(1)
        return (kcal / perStep).coerceAtLeast(0)
    }
}
