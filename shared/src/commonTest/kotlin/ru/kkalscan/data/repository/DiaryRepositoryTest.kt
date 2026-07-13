package ru.kkalscan.data.repository

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import ru.kkalscan.StatefulDiaryApi
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.offlineDiaryRepo
import kotlin.test.Test

class DiaryRepositoryTest {

    private val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
    private val repository = DiaryRepository(
        api = TestApiFixtures.api(),
        deviceIdStorage = storage,
        todayProvider = { TestApiFixtures.TODAY },
    )

    @Test
    fun getToday_returnsDiaryFromApi() = runTest {
        val day = repository.getToday()
        day.totalKcal shouldBe 350
        day.entries.size shouldBe 1
        day.scansLeft shouldBe 2
    }

    @Test
    fun addFromScan_refreshesDiary() = runTest {
        val dishes = listOf(Dish("Тест", 200, 350, 15.0, 10.0, 40.0))
        val day = repository.addFromScan("scan-id", MealType.lunch, dishes)
        day.entries.isNotEmpty() shouldBe true
    }

    @Test
    fun deleteEntry_callsApi() = runTest {
        repository.deleteEntry("entry-1")
        repository.getToday().entries.size shouldBe 1
    }

    @Test
    fun getToday_cachesResultInLocalStore() = runTest {
        val store = InMemoryDiaryLocalStore()
        val repo = offlineDiaryRepo(TestApiFixtures.api(), store)

        repo.getToday().totalKcal shouldBe 350
        store.getDay(TestApiFixtures.TODAY)!!.totalKcal shouldBe 350
    }

    @Test
    fun addFromScan_updatesLocalCache() = runTest {
        val store = InMemoryDiaryLocalStore()
        val api = StatefulDiaryApi(diaryDate = TestApiFixtures.TODAY)
        val repo = offlineDiaryRepo(api, store)
        val dishes = listOf(Dish("Тест", 200, 350, 15.0, 10.0, 40.0))

        repo.addFromScan("scan-id", MealType.lunch, dishes)

        store.getDay(TestApiFixtures.TODAY)!!.entries.isNotEmpty() shouldBe true
    }
}
