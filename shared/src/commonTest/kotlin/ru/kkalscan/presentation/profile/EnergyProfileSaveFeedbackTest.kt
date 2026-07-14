package ru.kkalscan.presentation.profile

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.domain.activity.EnergyProfile
import ru.kkalscan.offlineProfileRepo
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnergyProfileSaveFeedbackTest {

    @Test
    fun saveEnergyProfile_marksSavedAndRequestsClearFocus() = runTest {
        val vm = createOfflineProfileViewModel(offlineProfileRepo(FakeKkalScanApi()))
        try {
            val ok = vm.saveEnergyProfile(
                EnergyProfile(weightKg = 82.0, heightCm = 180.0, ageYears = 41),
            )
            ok shouldBe true

            val state = vm.state.value
            state.profileSaved shouldBe true
            state.clearEnergyFieldFocus shouldBe true
            state.energyProfile.weightKg shouldBe 82.0
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun clearProfileSaved_afterConsumeFocus_leavesSaveFlashOffAndFocusCleared() = runTest {
        val vm = createOfflineProfileViewModel(offlineProfileRepo(FakeKkalScanApi()))
        try {
            vm.saveEnergyProfile(EnergyProfile(weightKg = 82.0, heightCm = 180.0, ageYears = 41))
            vm.consumeClearEnergyFieldFocus()
            vm.clearProfileSaved()

            val state = vm.state.value
            state.profileSaved shouldBe false
            state.clearEnergyFieldFocus shouldBe false
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun consumeClearEnergyFieldFocus_isIdempotent() = runTest {
        val vm = createOfflineProfileViewModel(offlineProfileRepo(FakeKkalScanApi()))
        try {
            vm.saveEnergyProfile(EnergyProfile(weightKg = 90.0, heightCm = 175.0, ageYears = 35))

            vm.consumeClearEnergyFieldFocus()
            vm.state.value.clearEnergyFieldFocus shouldBe false
            vm.consumeClearEnergyFieldFocus()
            vm.state.value.clearEnergyFieldFocus shouldBe false
        } finally {
            vm.tearDownForTest()
        }
    }
}
