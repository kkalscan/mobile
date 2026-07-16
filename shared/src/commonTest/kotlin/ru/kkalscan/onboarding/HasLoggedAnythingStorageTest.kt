package ru.kkalscan.onboarding

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class HasLoggedAnythingStorageTest {

    @Test
    fun defaultsFalse_thenPersistsTrue() {
        val store = InMemoryHasLoggedAnythingStorage()
        store.hasLoggedAnything() shouldBe false
        store.markLoggedAnything()
        store.hasLoggedAnything() shouldBe true
    }
}
