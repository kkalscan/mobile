package ru.kkalscan.health

import io.kotest.matchers.shouldBe
import ru.kkalscan.presentation.diary.DiaryUiState
import kotlin.test.Test
import kotlin.test.assertFalse

class HealthConnectOnboardingPolicyTest {

    @Test
    fun shouldAutoRequest_disabledWithFeatureFlagOff() {
        val state = DiaryUiState(isLoading = false)
        HealthConnectOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe false
    }

    @Test
    fun shouldShowConnectButton_alwaysFalseWhenDisabled() {
        val state = DiaryUiState(isLoading = false, stepSensorAvailable = true)
        assertFalse(HealthConnectOnboardingPolicy.shouldShowConnectButton(state))
    }

    @Test
    fun controller_doesNotAutoRequestWhenDisabled() {
        val storage = InMemoryHealthConnectOnboardingStorage()
        val controller = HealthConnectOnboardingController(storage)
        val state = DiaryUiState(isLoading = false)
        var requested = false
        controller.tryAutoRequest(state) { requested = true }
        requested shouldBe false
        controller.autoRequestCount shouldBe 0
    }
}
