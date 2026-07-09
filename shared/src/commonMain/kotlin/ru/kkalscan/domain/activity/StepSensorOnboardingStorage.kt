package ru.kkalscan.domain.activity

interface StepSensorOnboardingStorage {
    fun wasInitialPromptShown(): Boolean
    fun markInitialPromptShown()
}

expect fun createStepSensorOnboardingStorage(): StepSensorOnboardingStorage
