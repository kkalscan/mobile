package ru.kkalscan.domain.activity

class InMemoryStepSensorOnboardingStorage(
    private var initialPromptShown: Boolean = false,
) : StepSensorOnboardingStorage {
    override fun wasInitialPromptShown(): Boolean = initialPromptShown

    override fun markInitialPromptShown() {
        initialPromptShown = true
    }
}
