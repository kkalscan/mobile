package ru.kkalscan.data.profile

import android.content.Context
import ru.kkalscan.data.storage.AndroidDeviceIdContext
import ru.kkalscan.domain.activity.EnergyProfile
import ru.kkalscan.domain.activity.Sex

actual fun createEnergyProfileStorage(): IEnergyProfileStorage =
    AndroidEnergyProfileStorage(AndroidDeviceIdContext.appContext)

private class AndroidEnergyProfileStorage(context: Context) : IEnergyProfileStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getProfile(): EnergyProfile? {
        if (!prefs.contains(KEY_WEIGHT_KG)) return null
        return EnergyProfile(
            sex = if (prefs.getString(KEY_SEX, Sex.Male.name) == Sex.Female.name) Sex.Female else Sex.Male,
            weightKg = prefs.getFloat(KEY_WEIGHT_KG, 0f).toDouble(),
            heightCm = prefs.getFloat(KEY_HEIGHT_CM, EnergyProfile.DEFAULT_HEIGHT_CM.toFloat()).toDouble(),
            ageYears = prefs.getInt(KEY_AGE_YEARS, EnergyProfile.DEFAULT_AGE_YEARS),
        ).normalized()
    }

    override fun saveProfile(profile: EnergyProfile) {
        val p = profile.normalized()
        prefs.edit()
            .putString(KEY_SEX, p.sex.name)
            .putFloat(KEY_WEIGHT_KG, p.weightKg.toFloat())
            .putFloat(KEY_HEIGHT_CM, p.heightCm.toFloat())
            .putInt(KEY_AGE_YEARS, p.ageYears)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "kkalscan_profile"
        const val KEY_SEX = "sex"
        const val KEY_WEIGHT_KG = "body_weight_kg"
        const val KEY_HEIGHT_CM = "height_cm"
        const val KEY_AGE_YEARS = "age_years"
    }
}
