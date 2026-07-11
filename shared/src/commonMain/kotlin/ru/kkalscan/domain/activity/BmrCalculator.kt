package ru.kkalscan.domain.activity

import kotlin.math.roundToInt

object BmrCalculator {
    /** Mifflin–St Jeor equation (kcal/day at rest). */
    fun dailyBmr(profile: EnergyProfile): Int {
        val p = profile.normalized()
        val base = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.ageYears
        val bmr = when (p.sex) {
            Sex.Male -> base + 5
            Sex.Female -> base - 161
        }
        return bmr.roundToInt().coerceAtLeast(800)
    }

    fun proratedBmr(profile: EnergyProfile, dayFraction: Double): Int {
        val fraction = dayFraction.coerceIn(0.0, 1.0)
        return (dailyBmr(profile) * fraction).roundToInt().coerceAtLeast(0)
    }
}
