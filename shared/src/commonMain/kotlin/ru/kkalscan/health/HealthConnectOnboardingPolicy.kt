package ru.kkalscan.health

import ru.kkalscan.presentation.diary.DiaryUiState

object HealthConnectOnboardingPolicy {
    fun shouldAutoRequest(
        state: DiaryUiState,
        initialPromptShown: Boolean,
    ): Boolean = HealthConnectFeature.ENABLED &&
        !state.isLoading &&
        !initialPromptShown

    fun shouldShowConnectButton(state: DiaryUiState): Boolean = false
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
