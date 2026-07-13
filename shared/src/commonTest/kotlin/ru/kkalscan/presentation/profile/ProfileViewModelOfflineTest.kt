package ru.kkalscan.presentation.profile

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.FailingProfileAndDiaryApi
import ru.kkalscan.FailingProfileApi
import ru.kkalscan.RecordingProfileFailingDiaryApi
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.data.local.InMemoryProfileLocalStore
import ru.kkalscan.offlineProfileRepo
import ru.kkalscan.sampleCachedDayWithScans
import ru.kkalscan.sampleCachedSubscription
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelOfflineTest {

    @Test
    fun refresh_offlineWithCache_showsStatusAndScansLeft() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        val diaryStore = InMemoryDiaryLocalStore()
        profileStore.upsert(sampleCachedSubscription(isPro = true))
        diaryStore.upsert(sampleCachedDayWithScans(scansLeft = 4))
        val vm = createOfflineProfileViewModel(
            offlineProfileRepo(FailingProfileAndDiaryApi(), profileStore, diaryStore),
        )
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.status.shouldNotBeNull()
            vm.state.value.status!!.isPro shouldBe true
            vm.state.value.scansLeft shouldBe 4
            vm.state.value.errorMessage.shouldBeNull()
            vm.state.value.isLoading shouldBe false
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun refresh_offlineWithoutCache_showsError() = runTest {
        val vm = createOfflineProfileViewModel(offlineProfileRepo(FailingProfileAndDiaryApi()))
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.status.shouldBeNull()
            vm.state.value.errorMessage.shouldNotBeNull()
            vm.state.value.isLoading shouldBe false
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun refresh_onlineUpdatesCachedSubscription() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        val diaryStore = InMemoryDiaryLocalStore()
        profileStore.upsert(sampleCachedSubscription(isPro = false))
        diaryStore.upsert(sampleCachedDayWithScans(scansLeft = 2))
        val vm = createOfflineProfileViewModel(
            offlineProfileRepo(
                RecordingProfileFailingDiaryApi(sampleCachedSubscription(isPro = true)),
                profileStore,
                diaryStore,
            ),
        )
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.status!!.isPro shouldBe true
            vm.state.value.scansLeft shouldBe 2
            vm.state.value.errorMessage.shouldBeNull()
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun refresh_offlineWithSubscriptionOnly_showsStatusWithoutScansLeft() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        profileStore.upsert(sampleCachedSubscription(isPro = false))
        val vm = createOfflineProfileViewModel(
            offlineProfileRepo(FailingProfileAndDiaryApi(), profileStore = profileStore),
        )
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.status.shouldNotBeNull()
            vm.state.value.status!!.isPro shouldBe false
            vm.state.value.scansLeft.shouldBeNull()
            vm.state.value.errorMessage.shouldBeNull()
        } finally {
            vm.tearDownForTest()
        }
    }
}
