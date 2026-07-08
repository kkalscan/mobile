package ru.kkalscan.health

import io.kotest.matchers.shouldBe
import ru.kkalscan.presentation.diary.DiaryUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthConnectOnboardingPolicyTest {

    @Test
    fun shouldAutoRequest_onFirstLaunchWhenHealthConnectAvailable() {
        val state = readyState(healthConnectAvailable = true, permissionsGranted = false)

        HealthConnectOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe true
    }

    @Test
    fun shouldAutoRequest_falseWhileLoading() {
        val state = readyState(isLoading = true, healthConnectAvailable = true, permissionsGranted = false)

        HealthConnectOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe false
    }

    @Test
    fun shouldAutoRequest_falseWhenPermissionsAlreadyGranted() {
        val state = readyState(healthConnectAvailable = true, permissionsGranted = true)

        HealthConnectOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe false
    }

    @Test
    fun shouldAutoRequest_falseWhenHealthConnectUnavailable() {
        val state = readyState(healthConnectAvailable = false, permissionsGranted = false)

        HealthConnectOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe false
    }

    @Test
    fun shouldAutoRequest_falseAfterInitialPromptWasShown() {
        val state = readyState(healthConnectAvailable = true, permissionsGranted = false)

        HealthConnectOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = true) shouldBe false
    }

    @Test
    fun shouldShowConnectButton_onlyWhenAvailableAndNotConnected() {
        val disconnected = readyState(healthConnectAvailable = true, permissionsGranted = false)
        val connected = readyState(healthConnectAvailable = true, permissionsGranted = true)
        val unavailable = readyState(healthConnectAvailable = false, permissionsGranted = false)

        assertTrue(HealthConnectOnboardingPolicy.shouldShowConnectButton(disconnected))
        assertFalse(HealthConnectOnboardingPolicy.shouldShowConnectButton(connected))
        assertFalse(HealthConnectOnboardingPolicy.shouldShowConnectButton(unavailable))
    }

    @Test
    fun controller_autoRequestsOnlyOnce_evenOnRepeatedLaunches() {
        val storage = InMemoryHealthConnectOnboardingStorage()
        val controller = HealthConnectOnboardingController(storage)
        val state = readyState(healthConnectAvailable = true, permissionsGranted = false)
        var requestCount = 0
        val request = { requestCount += 1 }

        controller.tryAutoRequest(state, request)
        controller.tryAutoRequest(state, request)
        controller.tryAutoRequest(state, request)

        requestCount shouldBe 1
        controller.autoRequestCount shouldBe 1
        storage.wasInitialPromptShown() shouldBe true
    }

    @Test
    fun controller_secondAppLaunch_doesNotAutoRequestAgain() {
        val storage = InMemoryHealthConnectOnboardingStorage(initialPromptShown = true)
        val controller = HealthConnectOnboardingController(storage)
        val state = readyState(healthConnectAvailable = true, permissionsGranted = false)
        var requestCount = 0

        controller.tryAutoRequest(state) { requestCount += 1 }

        requestCount shouldBe 0
        controller.autoRequestCount shouldBe 0
    }

  private fun readyState(
        isLoading: Boolean = false,
        healthConnectAvailable: Boolean,
        permissionsGranted: Boolean,
    ) = DiaryUiState(
        isLoading = isLoading,
        healthConnectAvailable = healthConnectAvailable,
        healthConnectPermissionsGranted = permissionsGranted,
    )
}
