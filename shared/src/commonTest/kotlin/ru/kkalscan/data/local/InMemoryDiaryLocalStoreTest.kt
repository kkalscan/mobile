package ru.kkalscan.data.local

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.sampleCachedDay
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryDiaryLocalStoreTest {

    private val today = TestApiFixtures.TODAY

    @Test
    fun upsert_andGetDay_roundTripsDiaryDay() = runTest {
        val store = InMemoryDiaryLocalStore()
        val day = sampleCachedDay()

        store.upsert(day)

        store.getDay(today)!!.totalKcal shouldBe 120
        store.getDay(today)!!.entries.first().dishes.first().name shouldBe "Каша"
    }

    @Test
    fun observeDay_emitsOnUpsert() = runTest {
        val store = InMemoryDiaryLocalStore()
        val day = sampleCachedDay()
        val values = mutableListOf<ru.kkalscan.domain.model.DiaryDay?>()
        val job = launch {
            store.observeDay(today).collect { values.add(it) }
        }
        advanceUntilIdle()
        values.last().shouldBeNull()

        store.upsert(day)
        advanceUntilIdle()

        values.last()!!.totalKcal shouldBe 120
        job.cancel()
    }

    @Test
    fun getDays_returnsOnlyRequestedDates() = runTest {
        val store = InMemoryDiaryLocalStore()
        store.upsert(sampleCachedDay(date = "2026-06-10", totalKcal = 100))
        store.upsert(sampleCachedDay(date = "2026-06-11", totalKcal = 200))

        val days = store.getDays(listOf("2026-06-10", "2026-06-12"))

        days.keys shouldHaveSize 1
        days["2026-06-10"]!!.totalKcal shouldBe 100
    }

    @Test
    fun upsert_overwritesExistingDay() = runTest {
        val store = InMemoryDiaryLocalStore()
        store.upsert(sampleCachedDay(totalKcal = 100))
        store.upsert(sampleCachedDay(totalKcal = 500))

        store.getDay(today)!!.totalKcal shouldBe 500
    }
}
