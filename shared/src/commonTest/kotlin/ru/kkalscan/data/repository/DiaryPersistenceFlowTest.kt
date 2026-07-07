package ru.kkalscan.data.repository

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.StatefulDiaryApi
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.data.storage.PersistentDeviceIdStorage
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.presentation.diary.createDiaryViewModelForTest
import ru.kkalscan.presentation.scan.ScanViewModel
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Regression: add dish → refresh → still visible today → add second → refresh → both visible.
 * Also covers device_id persistence across "page reload".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiaryPersistenceFlowTest {

    private val today = TestApiFixtures.TODAY
    private val tz = 180

    private fun repository(
        api: StatefulDiaryApi,
        storage: ru.kkalscan.data.storage.IDeviceIdStorage,
    ) = DiaryRepository(api, storage, todayProvider = { today })

    @Test
    fun addDish_refresh_stillShowsToday() = runTest {
        val api = StatefulDiaryApi(diaryDate = today)
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = repository(api, storage)

        repo.getToday(tz).entries shouldHaveSize 0

        val scan = api.scanPhoto(storage.getDeviceId(), byteArrayOf(1), tz)
        repo.addFromScan(scan.scanId, MealType.lunch, scan.dishes)

        val afterRefresh = repo.getToday(tz)
        afterRefresh.entries shouldHaveSize 1
        afterRefresh.entries.first().dishes.first().name shouldBe "Блюдо 1"
        afterRefresh.totalKcal shouldBe 100
    }

    @Test
    fun addTwoDishes_eachRefresh_keepsAllToday() = runTest {
        val api = StatefulDiaryApi(diaryDate = today)
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = repository(api, storage)

        val scan1 = api.scanPhoto(storage.getDeviceId(), byteArrayOf(1), tz)
        repo.addFromScan(scan1.scanId, MealType.breakfast, scan1.dishes)
        repo.getToday(tz).entries shouldHaveSize 1

        val scan2 = api.scanPhoto(storage.getDeviceId(), byteArrayOf(2), tz)
        repo.addFromScan(scan2.scanId, MealType.lunch, scan2.dishes)
        val afterSecondRefresh = repo.getToday(tz)
        afterSecondRefresh.entries shouldHaveSize 2
        afterSecondRefresh.totalKcal shouldBe 300
        afterSecondRefresh.entries.map { it.dishes.first().name } shouldBe listOf("Блюдо 1", "Блюдо 2")
    }

    @Test
    fun survivesReload_whenDeviceIdPersisted() = runTest {
        val api = StatefulDiaryApi(diaryDate = today)
        var stored: String? = null
        val storageBeforeReload = PersistentDeviceIdStorage(
            readStored = { stored },
            writeStored = { stored = it },
        )
        val repoBefore = repository(api, storageBeforeReload)

        val scan = api.scanPhoto(storageBeforeReload.getDeviceId(), byteArrayOf(1), tz)
        repoBefore.addFromScan(scan.scanId, MealType.lunch, scan.dishes)

        val storageAfterReload = PersistentDeviceIdStorage(
            readStored = { stored },
            writeStored = { stored = it },
        )
        val repoAfter = repository(api, storageAfterReload)

        storageAfterReload.getDeviceId() shouldBe storageBeforeReload.getDeviceId()
        repoAfter.getToday(tz).entries shouldHaveSize 1
    }

    @Test
    fun refreshAfterReload_withoutPersistedDeviceId_losesDiary_reproducesBug() = runTest {
        val api = StatefulDiaryApi(diaryDate = today)
        val firstSession = InMemoryDeviceIdStorage()
        val repoFirst = repository(api, firstSession)

        val scan = api.scanPhoto(firstSession.getDeviceId(), byteArrayOf(1), tz)
        repoFirst.addFromScan(scan.scanId, MealType.lunch, scan.dishes)
        repoFirst.getToday(tz).entries shouldHaveSize 1

        val secondSession = InMemoryDeviceIdStorage()
        val repoSecond = repository(api, secondSession)

        firstSession.getDeviceId() shouldNotBe secondSession.getDeviceId()
        repoSecond.getToday(tz).entries shouldHaveSize 0
    }

    @Test
    fun describeText_addToDiary_thenRefresh_showsEntry() = runTest {
        val api = StatefulDiaryApi(diaryDate = today)
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = repository(api, storage)

        repo.getToday(tz).entries shouldHaveSize 0

        val scan = api.describeFood(storage.getDeviceId(), "тарелка борща", tz)
        repo.addFromScan(scan.scanId, MealType.dinner, scan.dishes)

        val afterRefresh = repo.getToday(tz)
        afterRefresh.entries shouldHaveSize 1
        afterRefresh.entries.first().dishes.first().name shouldBe "Борщ с говядиной"
        afterRefresh.entries.first().mealType shouldBe MealType.dinner
        afterRefresh.totalKcal shouldBe 250
    }

    @Test
    fun viewModel_addToDiary_thenRefresh_showsEntries() = runTest {
        val api = StatefulDiaryApi(diaryDate = today)
        var stored: String? = null
        val storage = PersistentDeviceIdStorage(
            readStored = { stored },
            writeStored = { stored = it },
        )
        val diaryRepo = repository(api, storage)
        val scanRepo = ScanRepository(api, storage)
        val diaryVm = createDiaryViewModelForTest(diaryRepo, this)
        val scanVm = ScanViewModel(scanRepo, diaryRepo, this)
        advanceUntilIdle()

        diaryVm.state.value.day!!.entries shouldHaveSize 0

        scanVm.scanPhoto(byteArrayOf(1))
        advanceUntilIdle()
        scanVm.addToDiary().isSuccess shouldBe true
        diaryVm.refresh()
        advanceUntilIdle()
        diaryVm.state.value.day!!.entries shouldHaveSize 1

        scanVm.scanPhoto(byteArrayOf(2))
        advanceUntilIdle()
        scanVm.addToDiary().isSuccess shouldBe true
        diaryVm.refresh()
        advanceUntilIdle()
        diaryVm.state.value.day!!.entries shouldHaveSize 2
    }
}
