package ru.kkalscan.presentation.diary

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.FailingDiaryApi
import ru.kkalscan.RecordingDiaryApi
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.offlineDiaryRepo
import ru.kkalscan.sampleCachedDay
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelOfflineTest {

    @Test
    fun refresh_offlineWithCache_showsDayWithoutError() = runTest {
        val store = InMemoryDiaryLocalStore()
        store.upsert(sampleCachedDay(totalKcal = 420))
        val vm = createOfflineDiaryViewModel(offlineDiaryRepo(FailingDiaryApi(), store))
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.day.shouldNotBeNull()
            vm.state.value.day!!.totalKcal shouldBe 420
            vm.state.value.errorMessage.shouldBeNull()
            vm.state.value.isLoading shouldBe false
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun refresh_offlineWithoutCache_showsError() = runTest {
        val vm = createOfflineDiaryViewModel(offlineDiaryRepo(FailingDiaryApi()))
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.day.shouldBeNull()
            vm.state.value.errorMessage.shouldNotBeNull()
            vm.state.value.isLoading shouldBe false
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun refresh_onlineUpdatesCachedDay() = runTest {
        val store = InMemoryDiaryLocalStore()
        val cached = sampleCachedDay(totalKcal = 120)
        store.upsert(cached)
        val vm = createOfflineDiaryViewModel(
            offlineDiaryRepo(RecordingDiaryApi(cached.copy(totalKcal = 777)), store),
        )
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.day!!.totalKcal shouldBe 777
            vm.state.value.errorMessage.shouldBeNull()
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun init_refreshUsesObserveFlow_notBlockingOnNetwork() = runTest {
        val store = InMemoryDiaryLocalStore()
        store.upsert(sampleCachedDay())
        val vm = createOfflineDiaryViewModel(offlineDiaryRepo(FailingDiaryApi(), store))
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.day.shouldNotBeNull()
            vm.state.value.day!!.totalKcal shouldBe 120
        } finally {
            vm.tearDownForTest()
        }
    }
}
