package ru.kkalscan.data.local

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.sampleCachedSubscription
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryProfileLocalStoreTest {

    @Test
    fun upsert_andGetSubscription_roundTripsStatus() = runTest {
        val store = InMemoryProfileLocalStore()
        val status = sampleCachedSubscription(isPro = true)

        store.upsert(status)

        store.getSubscription()!!.isPro shouldBe true
    }

    @Test
    fun observeSubscription_emitsOnUpsert() = runTest {
        val store = InMemoryProfileLocalStore()
        val values = mutableListOf<ru.kkalscan.domain.model.SubscriptionStatus?>()
        val job = launch {
            store.observeSubscription().collect { values.add(it) }
        }
        advanceUntilIdle()
        values.last().shouldBeNull()

        store.upsert(sampleCachedSubscription(isPro = true))
        advanceUntilIdle()

        values.last()!!.isPro shouldBe true
        job.cancel()
    }

    @Test
    fun upsert_overwritesExistingStatus() = runTest {
        val store = InMemoryProfileLocalStore()
        store.upsert(sampleCachedSubscription(isPro = false))
        store.upsert(sampleCachedSubscription(isPro = true))

        store.getSubscription()!!.isPro shouldBe true
    }
}
