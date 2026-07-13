package ru.kkalscan.data.repository

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import ru.kkalscan.FailingDiaryApi
import ru.kkalscan.RecordingDiaryApi
import ru.kkalscan.StatefulDiaryApi
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.offlineDiaryRepo
import ru.kkalscan.sampleCachedDay
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryRepositoryOfflineTest {

    private val today = TestApiFixtures.TODAY

    @Test
    fun observeDay_emitsCacheThenNetworkUpdate() = runTest {
        val store = InMemoryDiaryLocalStore()
        val cached = sampleCachedDay(totalKcal = 120)
        store.upsert(cached)
        val api = RecordingDiaryApi(networkDay = cached.copy(totalKcal = 999))
        val repo = offlineDiaryRepo(api, store)

        val final = withTimeout(3_000) {
            repo.observeDay(today).first { !it.isRefreshing && it.day?.totalKcal == 999 }
        }
        final.day!!.totalKcal shouldBe 999
        api.getDiaryCalls shouldBe 1
    }

    @Test
    fun observeDay_offlineKeepsCache() = runTest {
        val store = InMemoryDiaryLocalStore()
        store.upsert(sampleCachedDay())
        val repo = offlineDiaryRepo(FailingDiaryApi(), store)

        val withCache = withTimeout(3_000) {
            repo.observeDay(today).first { it.day != null && !it.isRefreshing }
        }
        withCache.day!!.totalKcal shouldBe 120
        withCache.error.shouldBeNull()
    }

    @Test
    fun observeDay_offlineWithoutCache_emitsError() = runTest {
        val repo = offlineDiaryRepo(FailingDiaryApi())

        val failed = withTimeout(3_000) {
            repo.observeDay(today).first { !it.isRefreshing }
        }
        failed.day.shouldBeNull()
        failed.error.shouldNotBeNull()
        (failed.error as KkalScanException.Network).message shouldBe "offline"
    }

    @Test
    fun getDay_fallsBackToCacheWhenNetworkFails() = runTest {
        val store = InMemoryDiaryLocalStore()
        store.upsert(sampleCachedDay())
        val repo = offlineDiaryRepo(FailingDiaryApi(), store)

        repo.getDay(today).totalKcal shouldBe 120
    }

    @Test
    fun getDay_persistsNetworkResultToCache() = runTest {
        val store = InMemoryDiaryLocalStore()
        val api = StatefulDiaryApi(diaryDate = today)
        val repo = offlineDiaryRepo(api, store)

        repo.getDay(today).entries shouldHaveSize 0
        store.getDay(today)!!.entries shouldHaveSize 0

        val scan = api.scanPhoto(TestApiFixtures.DEVICE_ID, byteArrayOf(1), 180)
        repo.addFromScan(scan.scanId, MealType.lunch, scan.dishes)

        store.getDay(today)!!.entries shouldHaveSize 1
    }

    @Test
    fun syncActivity_upsertsLocalStoreWithoutExtraGetDiary() = runTest {
        val store = InMemoryDiaryLocalStore()
        val api = StatefulDiaryApi(diaryDate = today)
        val repo = offlineDiaryRepo(api, store)

        val day = repo.syncActivity(steps = 5000, kcal = 300, source = ActivitySource.DeviceSensor)
        day.activityKcal shouldBe 300
        day.activitySteps shouldBe 5000

        store.getDay(today)!!.activityKcal shouldBe 300
        store.getDay(today)!!.activitySteps shouldBe 5000
    }

    @Test
    fun addFromDishes_writesThroughToLocalStore() = runTest {
        val store = InMemoryDiaryLocalStore()
        val api = StatefulDiaryApi(diaryDate = today)
        val repo = offlineDiaryRepo(api, store)
        val dishes = listOf(Dish("Салат", 150, 80, 3.0, 5.0, 8.0))

        repo.addFromDishes(dishes, MealType.snack)

        store.getDay(today)!!.entries shouldHaveSize 1
        store.getDay(today)!!.totalKcal shouldBe 80
    }

    @Test
    fun getToday_afterSuccessfulFetch_servesFromCacheWhenOffline() = runTest {
        val store = InMemoryDiaryLocalStore()
        val api = StatefulDiaryApi(diaryDate = today)
        val repo = offlineDiaryRepo(api, store)

        repo.getToday().entries shouldHaveSize 0

        val scan = api.scanPhoto(TestApiFixtures.DEVICE_ID, byteArrayOf(1), 180)
        repo.addFromScan(scan.scanId, MealType.lunch, scan.dishes)

        offlineDiaryRepo(FailingDiaryApi(), store).getToday().entries shouldHaveSize 1
    }
}
