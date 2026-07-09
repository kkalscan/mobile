package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import ru.kkalscan.presentation.diary.DiaryUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StepSensorOnboardingPolicyTest {

    @Test
    fun shouldAutoRequest_onFirstLaunchWhenSensorAvailable() {
        val state = readyState(stepSensorAvailable = true, permissionGranted = false)
        StepSensorOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe true
    }

    @Test
    fun shouldAutoRequest_falseWhileLoading() {
        val state = readyState(isLoading = true, stepSensorAvailable = true, permissionGranted = false)
        StepSensorOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe false
    }

    @Test
    fun shouldAutoRequest_falseWhenPermissionGranted() {
        val state = readyState(stepSensorAvailable = true, permissionGranted = true)
        StepSensorOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe false
    }

    @Test
    fun shouldAutoRequest_falseWhenSensorUnavailable() {
        val state = readyState(stepSensorAvailable = false, permissionGranted = false)
        StepSensorOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = false) shouldBe false
    }

    @Test
    fun shouldAutoRequest_falseAfterInitialPromptShown() {
        val state = readyState(stepSensorAvailable = true, permissionGranted = false)
        StepSensorOnboardingPolicy.shouldAutoRequest(state, initialPromptShown = true) shouldBe false
    }

    @Test
    fun shouldShowPermissionButton_whenSensorAvailableWithoutPermission() {
        val state = readyState(stepSensorAvailable = true, permissionGranted = false)
        assertTrue(StepSensorOnboardingPolicy.shouldShowPermissionButton(state))
    }

    @Test
    fun shouldShowPermissionButton_falseWhenPermissionGranted() {
        val state = readyState(stepSensorAvailable = true, permissionGranted = true)
        assertFalse(StepSensorOnboardingPolicy.shouldShowPermissionButton(state))
    }

    @Test
    fun controller_requestsOnceAndMarksPromptShown() {
        val storage = InMemoryStepSensorOnboardingStorage()
        val controller = StepSensorOnboardingController(storage)
        val state = readyState(stepSensorAvailable = true, permissionGranted = false)
        var requested = 0
        controller.tryAutoRequest(state) { requested++ }
        requested shouldBe 1
        controller.autoRequestCount shouldBe 1
        assertTrue(storage.wasInitialPromptShown())
        controller.tryAutoRequest(state) { requested++ }
        requested shouldBe 1
    }

    private fun readyState(
        isLoading: Boolean = false,
        stepSensorAvailable: Boolean,
        permissionGranted: Boolean,
    ) = DiaryUiState(
        isLoading = isLoading,
        stepSensorAvailable = stepSensorAvailable,
        activityRecognitionGranted = permissionGranted,
    )
}
