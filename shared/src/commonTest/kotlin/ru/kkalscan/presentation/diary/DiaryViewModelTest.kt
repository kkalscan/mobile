package ru.kkalscan.presentation.diary

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.StatefulDiaryApi
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.MealType
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {

    @Test
    fun refresh_loadsDiary() = runTest {
        val api = TestApiFixtures.api()
        val repo = DiaryRepository(
            api,
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            todayProvider = { TestApiFixtures.TODAY },
        )
        val vm = createDiaryViewModel(repo, api)
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        vm.state.value.isLoading shouldBe false
        vm.state.value.day.shouldNotBeNull()
        vm.state.value.day!!.totalKcal shouldBe 350
        vm.state.value.activitySource shouldBe ActivitySource.Emulator
        vm.state.value.balance!!.activityKcal shouldBe 750
    }

    @Test
    fun deleteEntry_triggersRefresh() = runTest {
        val api = TestApiFixtures.api()
        val repo = DiaryRepository(
            api,
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            todayProvider = { TestApiFixtures.TODAY },
        )
        val vm = createDiaryViewModel(repo, api)
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()
        vm.deleteEntry("entry-1")
        advanceUntilIdle()

        vm.state.value.errorMessage shouldBe null
    }

    @Test
    fun onForeground_afterOvernightBackground_rollsDiaryOverToNewDay() = runTest {
        var today = "2026-07-03"
        val api = StatefulDiaryApi(
            diaryDate = "2026-07-03",
            todayProvider = { today },
        )
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = CountingDiaryRepository(
            DiaryRepository(api, storage, todayProvider = { today }),
        )
        val vm = createDiaryViewModel(repo, api)
        advanceUntilIdle()

        val scan = api.scanPhoto(storage.getDeviceId(), byteArrayOf(1), 0)
        repo.addFromScan(scan.scanId, MealType.lunch, scan.dishes)
        vm.refresh()
        advanceUntilIdle()

        vm.state.value.date shouldBe "2026-07-03"
        vm.state.value.day!!.date shouldBe "2026-07-03"
        vm.state.value.day!!.entries shouldHaveSize 1

        val fetchesBefore = repo.getTodayCalls
        today = "2026-07-04"

        vm.onForeground()
        advanceUntilIdle()

        repo.getTodayCalls shouldBe fetchesBefore + 1
        vm.state.value.date shouldBe "2026-07-04"
        vm.state.value.day!!.date shouldBe "2026-07-04"
        vm.state.value.day!!.entries shouldHaveSize 0
    }

    @Test
    fun onForeground_sameDay_refreshesActivityOnly() = runTest {
        val today = "2026-07-03"
        val api = StatefulDiaryApi(
            diaryDate = today,
            todayProvider = { today },
        )
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = CountingDiaryRepository(
            DiaryRepository(api, storage, todayProvider = { today }),
        )
        val vm = createDiaryViewModel(repo, api)
        vm.refresh()
        advanceUntilIdle()

        val fetchesBefore = repo.getTodayCalls
        vm.onForeground()
        advanceUntilIdle()

        repo.getTodayCalls shouldBe fetchesBefore
        vm.state.value.activitySource shouldBe ActivitySource.Emulator
    }

    @Test
    fun usesBackendEmulatorForActivity() = runTest {
        val api = TestApiFixtures.api()
        val repo = DiaryRepository(
            api,
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            todayProvider = { TestApiFixtures.TODAY },
        )
        val vm = createDiaryViewModel(repo, api)
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        vm.state.value.activitySource shouldBe ActivitySource.Emulator
        vm.state.value.balance!!.activityKcal shouldBe 750
    }

    @Test
    fun diaryUiState_exposesPermissionButtonFromPolicy() {
        DiaryUiState(
            stepSensorAvailable = true,
            activityRecognitionGranted = false,
        ).showActivityPermissionButton shouldBe true

        DiaryUiState(
            stepSensorAvailable = true,
            activityRecognitionGranted = true,
        ).showActivityPermissionButton shouldBe false
    }
}

private class CountingDiaryRepository(
    private val delegate: IDiaryRepository,
) : IDiaryRepository by delegate {
    var getTodayCalls = 0
        private set

    override fun observeToday(timezoneOffsetMinutes: Int) = run {
        getTodayCalls++
        delegate.observeToday(timezoneOffsetMinutes)
    }

    override suspend fun getToday(timezoneOffsetMinutes: Int): DiaryDay {
        getTodayCalls++
        return delegate.getToday(timezoneOffsetMinutes)
    }
}
