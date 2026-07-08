package ru.kkalscan.health

class InMemoryHealthConnectOnboardingStorage(
    initialPromptShown: Boolean = false,
) : HealthConnectOnboardingStorage {
    var initialPromptShown: Boolean = initialPromptShown
        private set

    override fun wasInitialPromptShown(): Boolean = initialPromptShown

    override fun markInitialPromptShown() {
        initialPromptShown = true
    }
}
