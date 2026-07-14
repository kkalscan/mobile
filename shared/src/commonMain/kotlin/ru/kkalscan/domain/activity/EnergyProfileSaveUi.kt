package ru.kkalscan.domain.activity

/**
 * Pure rules for the energy-profile editor Save button.
 * Save is active only while the draft differs from the last saved profile.
 */
object EnergyProfileSaveUi {
    fun parseDraft(
        weightText: String,
        heightText: String,
        ageText: String,
    ): EnergyProfile? {
        val weight = weightText.toDoubleOrNull() ?: return null
        val height = heightText.toDoubleOrNull() ?: return null
        val age = ageText.toIntOrNull() ?: return null
        return EnergyProfile(weightKg = weight, heightCm = height, ageYears = age).normalized()
    }

    fun isSaveEnabled(
        savedProfile: EnergyProfile,
        weightText: String,
        heightText: String,
        ageText: String,
        justSaved: Boolean,
    ): Boolean {
        if (justSaved) return false
        val draft = parseDraft(weightText, heightText, ageText) ?: return false
        return draft != savedProfile.normalized()
    }

    fun saveButtonLabel(justSaved: Boolean): String =
        if (justSaved) "Сохранено" else "Сохранить"
}
