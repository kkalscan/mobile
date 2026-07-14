package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Spec for energy-profile editor UX:
 * - Save stays inactive while fields match the last saved profile (initial / after save).
 * - Save becomes active only after the user edits weight, height, or age.
 * - After a successful save, even once the "Сохранено" flash clears, Save stays inactive.
 */
class EnergyProfileSaveUiTest {

    private val saved = EnergyProfile(weightKg = 70.0, heightCm = 175.0, ageYears = 35)

    @Test
    fun saveDisabled_whenFieldsMatchSavedProfile() {
        EnergyProfileSaveUi.isSaveEnabled(
            savedProfile = saved,
            weightText = "70",
            heightText = "175",
            ageText = "35",
            justSaved = false,
        ) shouldBe false
    }

    @Test
    fun saveEnabled_afterWeightEdited() {
        EnergyProfileSaveUi.isSaveEnabled(
            savedProfile = saved,
            weightText = "75",
            heightText = "175",
            ageText = "35",
            justSaved = false,
        ) shouldBe true
    }

    @Test
    fun saveEnabled_afterHeightEdited() {
        EnergyProfileSaveUi.isSaveEnabled(
            savedProfile = saved,
            weightText = "70",
            heightText = "180",
            ageText = "35",
            justSaved = false,
        ) shouldBe true
    }

    @Test
    fun saveEnabled_afterAgeEdited() {
        EnergyProfileSaveUi.isSaveEnabled(
            savedProfile = saved,
            weightText = "70",
            heightText = "175",
            ageText = "40",
            justSaved = false,
        ) shouldBe true
    }

    @Test
    fun saveDisabled_whileJustSavedFlash_evenIfDraftStillLooksDirty() {
        // Guard: flash must force inactive; dirty without matching profile shouldn't keep Save active.
        EnergyProfileSaveUi.isSaveEnabled(
            savedProfile = saved,
            weightText = "75",
            heightText = "175",
            ageText = "35",
            justSaved = true,
        ) shouldBe false
    }

    @Test
    fun saveDisabled_afterSuccessfulSave_whenFlashCleared() {
        val afterSave = EnergyProfile(weightKg = 75.0, heightCm = 175.0, ageYears = 35)
        // Reproduces the bug: clearProfileSaved() must NOT re-enable Save if nothing else changed.
        EnergyProfileSaveUi.isSaveEnabled(
            savedProfile = afterSave,
            weightText = "75",
            heightText = "175",
            ageText = "35",
            justSaved = false,
        ) shouldBe false
    }

    @Test
    fun saveDisabled_whenDraftInvalid() {
        EnergyProfileSaveUi.isSaveEnabled(
            savedProfile = saved,
            weightText = "",
            heightText = "175",
            ageText = "35",
            justSaved = false,
        ) shouldBe false
    }

    @Test
    fun saveLabel_showsSaved_duringFlash() {
        EnergyProfileSaveUi.saveButtonLabel(justSaved = true) shouldBe "Сохранено"
        EnergyProfileSaveUi.saveButtonLabel(justSaved = false) shouldBe "Сохранить"
    }
}
