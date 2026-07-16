package ru.kkalscan.onboarding

/** Marks permanent first-log flag after a successful food or workout add. */
class FirstLogTracker(
    private val storage: HasLoggedAnythingStorage,
) {
    fun onFoodOrWorkoutLogged() {
        storage.markLoggedAnything()
    }
}
