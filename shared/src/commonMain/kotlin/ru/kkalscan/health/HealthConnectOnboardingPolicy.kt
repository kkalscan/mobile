package ru.kkalscan.health

import ru.kkalscan.presentation.diary.DiaryUiState

object HealthConnectOnboardingPolicy {
    fun shouldAutoRequest(
        state: DiaryUiState,
        initialPromptShown: Boolean,
    ): Boolean =
        !state.isLoading &&
            state.healthConnectAvailable &&
            !state.healthConnectPermissionsGranted &&
            !initialPromptShown

    fun shouldShowConnectButton(state: DiaryUiState): Boolean =
        state.healthConnectAvailable && !state.healthConnectPermissionsGranted
}

class HealthConnectOnboardingController(
    private val storage: HealthConnectOnboardingStorage,
) {
    var autoRequestCount: Int = 0
        private set

    fun tryAutoRequest(state: DiaryUiState, request: () -> Unit) {
        if (!HealthConnectOnboardingPolicy.shouldAutoRequest(state, storage.wasInitialPromptShown())) return
        storage.markInitialPromptShown()
        autoRequestCount++
        request()
    }
}
