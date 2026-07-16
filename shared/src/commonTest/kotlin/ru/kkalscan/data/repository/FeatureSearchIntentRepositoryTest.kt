package ru.kkalscan.data.repository

import kotlinx.coroutines.test.runTest
import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureSearchIntentRepositoryTest {

    @Test
    fun classifyIntent_foodQuery_true() = runTest {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId("d1") }
        val repo = FeatureSearchRepository(FakeKkalScanApi(), storage)
        val result = repo.classifyIntent("борщ")
        assertTrue(result.isFoodIntent)
    }

    @Test
    fun classifyIntent_featureQuery_false() = runTest {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId("d1") }
        val repo = FeatureSearchRepository(FakeKkalScanApi(), storage)
        val result = repo.classifyIntent("профиль")
        assertFalse(result.isFoodIntent)
    }
}
