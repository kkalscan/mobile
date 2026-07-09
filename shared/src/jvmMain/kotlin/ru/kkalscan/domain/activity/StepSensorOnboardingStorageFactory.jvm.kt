package ru.kkalscan.domain.activity

actual fun createStepSensorOnboardingStorage(): StepSensorOnboardingStorage =
    InMemoryStepSensorOnboardingStorage(initialPromptShown = true)
