package ru.kkalscan.data.storage

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import kotlin.test.Test

class PersistentDeviceIdStorageTest {

    @Test
    fun reusesStoredDeviceId() {
        var stored: String? = null
        val storage = PersistentDeviceIdStorage(
            readStored = { stored },
            writeStored = { stored = it },
        )

        val first = storage.getDeviceId()
        first.shouldNotBeBlank()
        stored shouldBe first

        val second = PersistentDeviceIdStorage(
            readStored = { stored },
            writeStored = { stored = it },
        ).getDeviceId()

        second shouldBe first
    }
}
