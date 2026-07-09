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
            { TestApiFixtures.TODAY },
        )
        val vm = createDiaryViewModelForTest(repo, this, api)
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        vm.state.value.isLoading shouldBe false
        vm.state.value.day.shouldNotBeNull()
        vm.state.value.day!!.totalKcal shouldBe 350
        vm.state.value.activitySource shouldBe ActivitySource.Emulator
        vm.state.value.balance!!.activityKcal shouldBe 400
    }

    @Test
    fun deleteEntry_triggersRefresh() = runTest {
        val api = TestApiFixtures.api()
        val repo = DiaryRepository(
            api,
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            { TestApiFixtures.TODAY },
        )
        val vm = createDiaryViewModelForTest(repo, this, api)
        vm.refresh()
        vm.deleteEntry("entry-1")

        vm.state.value.errorMessage shouldBe null
    }

    @Test
    fun onForeground_afterOvernightBackground_rollsDiaryOverToNewDay() = runTest {
        var today = "2026-07-03"
        val api = StatefulDiaryApi(diaryDate = "2026-07-03")
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = CountingDiaryRepository(
            DiaryRepository(api, storage, todayProvider = { today }),
        )
        val vm = createDiaryViewModelForTest(repo, this, api)
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
        val api = StatefulDiaryApi(diaryDate = today)
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = CountingDiaryRepository(
            DiaryRepository(api, storage, todayProvider = { today }),
        )
        val counter = FakeLocalStepCounter(
            sensorAvailable = true,
            permissionGranted = true,
            cumulativeSteps = 5_000,
        )
        val vm = createDiaryViewModelForTest(repo, this, api, localStepCounter = counter)
        advanceUntilIdle()

        val fetchesBefore = repo.getTodayCalls
        val readsBefore = counter.readCalls

        counter.cumulativeSteps = 7_000
        vm.onForeground()
        advanceUntilIdle()

        repo.getTodayCalls shouldBe fetchesBefore
        counter.readCalls shouldBe readsBefore + 1
        vm.state.value.activitySource shouldBe ActivitySource.DeviceSensor
        vm.state.value.steps shouldBe 2000
    }

    @Test
    fun sensorPreferredOverEmulator() = runTest {
        val api = TestApiFixtures.api()
        val repo = DiaryRepository(
            api,
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            { TestApiFixtures.TODAY },
        )
        val counter = FakeLocalStepCounter(
            sensorAvailable = true,
            permissionGranted = true,
            cumulativeSteps = 10_000,
        )
        val vm = createDiaryViewModelForTest(repo, this, api, localStepCounter = counter)
        advanceUntilIdle()
        counter.cumulativeSteps = 14_000
        vm.refreshActivityOnly()
        advanceUntilIdle()

        vm.state.value.activitySource shouldBe ActivitySource.DeviceSensor
        vm.state.value.balance!!.activityKcal shouldBe 160
    }

    @Test
    fun fallsBackToEmulatorWithoutPermission() = runTest {
        val api = TestApiFixtures.api()
        val repo = DiaryRepository(
            api,
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            { TestApiFixtures.TODAY },
        )
        val counter = FakeLocalStepCounter(
            sensorAvailable = true,
            permissionGranted = false,
            cumulativeSteps = 10_000,
        )
        val vm = createDiaryViewModelForTest(repo, this, api, localStepCounter = counter)
        advanceUntilIdle()

        vm.state.value.activitySource shouldBe ActivitySource.Emulator
        vm.state.value.balance!!.activityKcal shouldBe 400
        vm.state.value.showActivityPermissionButton shouldBe true
    }
}

private class CountingDiaryRepository(
    private val delegate: IDiaryRepository,
) : IDiaryRepository by delegate {
    var getTodayCalls = 0
        private set

    override suspend fun getToday(timezoneOffsetMinutes: Int): DiaryDay {
        getTodayCalls++
        return delegate.getToday(timezoneOffsetMinutes)
    }
}
