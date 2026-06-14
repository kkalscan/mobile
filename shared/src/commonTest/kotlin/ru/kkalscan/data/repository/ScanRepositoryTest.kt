package ru.kkalscan.data.repository

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import kotlin.test.Test

class ScanRepositoryTest {

    private val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
    private val repository = ScanRepository(TestApiFixtures.api(), storage)

    @Test
    fun scanPhoto_returnsResult() = runTest {
        val result = repository.scanPhoto(byteArrayOf(1, 2, 3))
        result.scanId shouldBe "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        result.totalKcal shouldBe 350
        result.scansLeft shouldBe 3
    }

    @Test
    fun grantAdBonus_increasesScans() = runTest {
        val bonus = repository.grantAdBonus()
        bonus.scansLeft shouldBe 5
        bonus.bonusGranted shouldBe true
    }
}
