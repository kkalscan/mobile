package ru.kkalscan.onboarding

interface HasLoggedAnythingStorage {
    fun hasLoggedAnything(): Boolean
    fun markLoggedAnything()
}

class InMemoryHasLoggedAnythingStorage(
    private var logged: Boolean = false,
) : HasLoggedAnythingStorage {
    override fun hasLoggedAnything(): Boolean = logged

    override fun markLoggedAnything() {
        logged = true
    }
}

expect fun createHasLoggedAnythingStorage(): HasLoggedAnythingStorage
