package ru.kkalscan.health

actual fun createHealthConnectOnboardingStorage(): HealthConnectOnboardingStorage =
    InMemoryHealthConnectOnboardingStorage(initialPromptShown = true)
