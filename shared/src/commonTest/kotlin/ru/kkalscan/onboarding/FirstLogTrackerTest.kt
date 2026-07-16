package ru.kkalscan.onboarding

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FirstLogTrackerTest {

    @Test
    fun onFoodOrWorkoutLogged_marksStorage() {
        val storage = InMemoryHasLoggedAnythingStorage()
        val tracker = FirstLogTracker(storage)

        storage.hasLoggedAnything() shouldBe false
        tracker.onFoodOrWorkoutLogged()
        storage.hasLoggedAnything() shouldBe true
    }
}
