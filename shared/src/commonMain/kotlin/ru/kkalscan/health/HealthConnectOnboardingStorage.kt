package ru.kkalscan.health

interface HealthConnectOnboardingStorage {
    fun wasInitialPromptShown(): Boolean
    fun markInitialPromptShown()
}

expect fun createHealthConnectOnboardingStorage(): HealthConnectOnboardingStorage
