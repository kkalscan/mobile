package ru.kkalscan.data.steps

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class StepCounterStoreTest {

    @Test
    fun computesStepsFromBaseline() = runTest {
        val counter = FakeCounter(cumulative = 12_000)
        val storage = InMemoryStepBaselineStorage()
        storage.setBaseline("2026-07-09", 10_000)
        val store = StepCounterStore(counter, storage) { "2026-07-09" }

        store.readTodaySteps() shouldBe 2000
    }

    @Test
    fun setsBaselineOnFirstReadOfDay() = runTest {
        val counter = FakeCounter(cumulative = 8_000)
        val storage = InMemoryStepBaselineStorage()
        val store = StepCounterStore(counter, storage) { "2026-07-09" }

        store.readTodaySteps() shouldBe 0
        storage.getBaseline("2026-07-09") shouldBe 8_000
    }

    @Test
    fun returnsNullWithoutPermission() = runTest {
        val counter = FakeCounter(cumulative = 8_000, permission = false)
        val store = StepCounterStore(counter, InMemoryStepBaselineStorage()) { "2026-07-09" }

        store.readTodaySteps() shouldBe null
    }

    private class FakeCounter(
        private var cumulative: Long,
        private val available: Boolean = true,
        private val permission: Boolean = true,
    ) : ILocalStepCounter {
        override suspend fun isSensorAvailable(): Boolean = available
        override suspend fun hasPermission(): Boolean = permission
        override suspend fun readCumulativeSteps(): Long? = cumulative
    }
}
