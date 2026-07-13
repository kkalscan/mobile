package ru.kkalscan.data.repository

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import ru.kkalscan.FailingDiaryApi
import ru.kkalscan.FailingProfileAndDiaryApi
import ru.kkalscan.FailingProfileApi
import ru.kkalscan.RecordingProfileApi
import ru.kkalscan.RecordingProfileFailingDiaryApi
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.data.local.InMemoryProfileLocalStore
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.offlineProfileRepo
import ru.kkalscan.sampleCachedDayWithScans
import ru.kkalscan.sampleCachedSubscription
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryOfflineTest {

    @Test
    fun observeProfile_offlineKeepsCachedSubscription() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        profileStore.upsert(sampleCachedSubscription(isPro = true))
        val repo = offlineProfileRepo(FailingProfileApi(), profileStore = profileStore)

        val resource = withTimeout(3_000) {
            repo.observeProfile().first { it.status != null && !it.isRefreshing }
        }

        resource.status!!.isPro shouldBe true
        resource.error.shouldBeNull()
    }

    @Test
    fun observeProfile_offlineWithDiaryCache_showsScansLeft() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        val diaryStore = InMemoryDiaryLocalStore()
        profileStore.upsert(sampleCachedSubscription())
        diaryStore.upsert(sampleCachedDayWithScans(scansLeft = 5))
        val repo = offlineProfileRepo(
            FailingProfileAndDiaryApi(),
            profileStore = profileStore,
            diaryStore = diaryStore,
        )

        val resource = withTimeout(3_000) {
            repo.observeProfile().first { !it.isRefreshing && it.scansLeft != null }
        }

        resource.scansLeft shouldBe 5
        resource.status.shouldNotBeNull()
        resource.error.shouldBeNull()
    }

    @Test
    fun observeProfile_offlineWithoutCache_emitsError() = runTest {
        val repo = offlineProfileRepo(FailingProfileAndDiaryApi())

        val resource = withTimeout(3_000) {
            repo.observeProfile().first { !it.isRefreshing }
        }

        resource.status.shouldBeNull()
        resource.scansLeft.shouldBeNull()
        resource.error.shouldNotBeNull()
        (resource.error as KkalScanException.Network).message shouldBe "offline"
    }

    @Test
    fun observeProfile_onlineUpdatesCachedSubscription() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        profileStore.upsert(sampleCachedSubscription(isPro = false))
        val diaryStore = InMemoryDiaryLocalStore()
        diaryStore.upsert(sampleCachedDayWithScans(scansLeft = 2))
        val api = RecordingProfileFailingDiaryApi(sampleCachedSubscription(isPro = true))
        val repo = offlineProfileRepo(api, profileStore = profileStore, diaryStore = diaryStore)

        val resource = withTimeout(3_000) {
            repo.observeProfile().first { it.status?.isPro == true && !it.isRefreshing }
        }

        resource.status!!.isPro shouldBe true
        resource.scansLeft shouldBe 2
        api.getSubscriptionStatusCalls shouldBe 1
    }

    @Test
    fun getStatus_fallsBackToCacheWhenNetworkFails() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        profileStore.upsert(sampleCachedSubscription(isPro = true))
        val repo = offlineProfileRepo(FailingProfileApi(), profileStore = profileStore)

        repo.getStatus().isPro shouldBe true
    }

    @Test
    fun observeProfile_subscriptionOfflineDiaryOnline_usesDiaryScansLeft() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        profileStore.upsert(sampleCachedSubscription(isPro = false))
        val api = RecordingProfileApi(sampleCachedSubscription(isPro = true))
        val repo = offlineProfileRepo(
            api,
            profileStore = profileStore,
            diaryStore = InMemoryDiaryLocalStore(),
        )

        val resource = withTimeout(3_000) {
            repo.observeProfile().first { !it.isRefreshing && it.scansLeft != null }
        }

        resource.status!!.isPro shouldBe true
        resource.scansLeft shouldBe 3
    }

    @Test
    fun observeProfile_diaryOfflineWithCache_keepsSubscriptionAndScansLeft() = runTest {
        val profileStore = InMemoryProfileLocalStore()
        val diaryStore = InMemoryDiaryLocalStore()
        profileStore.upsert(sampleCachedSubscription(isPro = true))
        diaryStore.upsert(sampleCachedDayWithScans(scansLeft = 7))
        val repo = offlineProfileRepo(FailingProfileAndDiaryApi(), profileStore = profileStore, diaryStore = diaryStore)

        val resource = withTimeout(3_000) {
            repo.observeProfile().first { !it.isRefreshing && it.status != null && it.scansLeft != null }
        }

        resource.status!!.isPro shouldBe true
        resource.scansLeft shouldBe 7
        resource.error.shouldBeNull()
    }
}
