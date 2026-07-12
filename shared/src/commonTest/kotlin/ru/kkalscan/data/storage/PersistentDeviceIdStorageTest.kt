package ru.kkalscan.data.storage

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

    @Test
    fun usesGenerateIdWhenNothingStored() {
        var stored: String? = null
        val storage = PersistentDeviceIdStorage(
            readStored = { stored },
            writeStored = { stored = it },
            generateId = { "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee" },
        )

        storage.getDeviceId() shouldBe "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        stored shouldBe "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    }

    @Test
    fun prefersStoredIdOverGenerateId() {
        var stored: String? = "11111111-1111-1111-1111-111111111111"
        val storage = PersistentDeviceIdStorage(
            readStored = { stored },
            writeStored = { stored = it },
            generateId = { "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee" },
        )

        storage.getDeviceId() shouldBe "11111111-1111-1111-1111-111111111111"
    }

    @Test
    fun sameStableKey_yieldsSameDeviceId_acrossReinstall() {
        val firstInstall = deviceIdFromAndroidId("deadbeef00001111")
        val afterReinstall = deviceIdFromAndroidId("deadbeef00001111")
        val otherDevice = deviceIdFromAndroidId("cafebabe00002222")

        firstInstall shouldBe afterReinstall
        firstInstall shouldNotBe otherDevice
        firstInstall.shouldNotBeBlank()
    }
}
