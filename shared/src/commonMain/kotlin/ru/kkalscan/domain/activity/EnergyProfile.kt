package ru.kkalscan.domain.activity

enum class Sex {
    Male,
    Female,
}

data class EnergyProfile(
    val sex: Sex = Sex.Male,
    val weightKg: Double = DEFAULT_WEIGHT_KG,
    val heightCm: Double = DEFAULT_HEIGHT_CM,
    val ageYears: Int = DEFAULT_AGE_YEARS,
) {
    fun normalized(): EnergyProfile = copy(
        weightKg = weightKg.coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG),
        heightCm = heightCm.coerceIn(MIN_HEIGHT_CM, MAX_HEIGHT_CM),
        ageYears = ageYears.coerceIn(MIN_AGE_YEARS, MAX_AGE_YEARS),
    )

    companion object {
        const val DEFAULT_WEIGHT_KG = 70.0
        const val DEFAULT_HEIGHT_CM = 175.0
        const val DEFAULT_AGE_YEARS = 35
        const val MIN_WEIGHT_KG = 40.0
        const val MAX_WEIGHT_KG = 200.0
        const val MIN_HEIGHT_CM = 120.0
        const val MAX_HEIGHT_CM = 230.0
        const val MIN_AGE_YEARS = 14
        const val MAX_AGE_YEARS = 100
    }
}
