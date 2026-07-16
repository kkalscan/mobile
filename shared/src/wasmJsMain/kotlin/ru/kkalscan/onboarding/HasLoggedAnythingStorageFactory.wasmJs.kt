package ru.kkalscan.onboarding

actual fun createHasLoggedAnythingStorage(): HasLoggedAnythingStorage =
    InMemoryHasLoggedAnythingStorage()
