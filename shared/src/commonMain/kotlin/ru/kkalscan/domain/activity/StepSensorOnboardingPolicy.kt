package ru.kkalscan.domain.activity

import ru.kkalscan.presentation.diary.DiaryUiState

object StepSensorOnboardingPolicy {
    fun shouldAutoRequest(
        state: DiaryUiState,
        initialPromptShown: Boolean,
    ): Boolean =
        StepSensorFeature.ENABLED &&
            !state.isLoading &&
            state.stepSensorAvailable &&
            !state.activityRecognitionGranted &&
            !initialPromptShown

    fun shouldShowPermissionButton(state: DiaryUiState): Boolean =
        StepSensorFeature.ENABLED &&
            state.stepSensorAvailable &&
            !state.activityRecognitionGranted
}

class StepSensorOnboardingController(
    private val storage: StepSensorOnboardingStorage,
) {
    var autoRequestCount: Int = 0
        private set

    fun tryAutoRequest(state: DiaryUiState, request: () -> Unit) {
        if (!StepSensorOnboardingPolicy.shouldAutoRequest(state, storage.wasInitialPromptShown())) return
        storage.markInitialPromptShown()
        autoRequestCount++
        request()
    }
}
