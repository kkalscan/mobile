package ru.kkalscan.presentation.scan

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.StatefulDiaryApi
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.repository.ScanRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.presentation.diary.createDiaryViewModelForTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DescribeFoodFlowTest {

    private val today = "2026-07-04"

    @Test
    fun describeText_addToDiary_persistsEntry() = runTest {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId("11111111-1111-1111-1111-111111111111") }
        val api = FakeKkalScanApi(todayProvider = { today })
        val scanRepo = ScanRepository(api, storage)
        val diaryRepo = DiaryRepository(api, storage, todayProvider = { today })
        val scanVm = ScanViewModel(scanRepo, diaryRepo, this)

        scanVm.describeText("тарелка борща")
        advanceUntilIdle()

        scanVm.state.value.result.shouldNotBeNull()
        scanVm.state.value.result!!.dishes.first().name shouldBe "Борщ с говядиной"
        scanVm.state.value.descriptionText shouldBe "тарелка борща"
        scanVm.state.value.photoBytes shouldBe null

        scanVm.selectMealType(MealType.lunch)
        scanVm.addToDiary().isSuccess shouldBe true
        advanceUntilIdle()

        val day = diaryRepo.getToday()
        day.entries shouldHaveSize 1
        day.entries.first().dishes.first().name shouldBe "Борщ с говядиной"
        day.entries.first().mealType shouldBe MealType.lunch
        day.totalKcal shouldBe 250
    }

    @Test
    fun describeText_addToDiary_diaryViewModelRefresh_showsEntry() = runTest {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val api = StatefulDiaryApi(diaryDate = today)
        val scanRepo = ScanRepository(api, storage)
        val diaryRepo = DiaryRepository(api, storage, todayProvider = { today })
        val scanVm = ScanViewModel(scanRepo, diaryRepo, this)
        val diaryVm = createDiaryViewModelForTest(diaryRepo, this, api, storage)
        advanceUntilIdle()

        diaryVm.state.value.day!!.entries shouldHaveSize 0

        scanVm.describeText("тарелка борща")
        advanceUntilIdle()
        scanVm.selectMealType(MealType.dinner)
        scanVm.addToDiary().isSuccess shouldBe true

        diaryVm.refresh()
        advanceUntilIdle()

        diaryVm.state.value.day!!.entries shouldHaveSize 1
        diaryVm.state.value.day!!.entries.first().dishes.first().name shouldBe "Борщ с говядиной"
        diaryVm.state.value.day!!.totalKcal shouldBe 250
    }
}
