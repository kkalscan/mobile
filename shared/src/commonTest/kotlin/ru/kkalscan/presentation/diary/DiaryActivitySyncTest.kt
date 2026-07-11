package ru.kkalscan.presentation.diary

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.StatefulDiaryApi
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.steps.InMemoryStepBaselineStorage
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.domain.activity.ActivitySource
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryActivitySyncTest {

    @Test
    fun refresh_syncsDeviceSensorActivityIntoDiary() = runTest {
        val api = StatefulDiaryApi(
            diaryDate = TestApiFixtures.TODAY,
            todayProvider = { TestApiFixtures.TODAY },
        )
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = DiaryRepository(api, storage, todayProvider = { TestApiFixtures.TODAY })
        val baselineStorage = InMemoryStepBaselineStorage().apply {
            setBaseline(TestApiFixtures.TODAY, 0L)
        }
        val stepCounter = FakeLocalStepCounter(
            sensorAvailable = true,
            permissionGranted = true,
            cumulativeSteps = 8750L,
        )
        val vm = createDiaryViewModelForTest(
            repo,
            this,
            api,
            storage,
            localStepCounter = stepCounter,
            stepBaselineStorage = baselineStorage,
        )
        advanceUntilIdle()

        vm.state.value.day!!.activityKcal shouldBeGreaterThan 0
        (vm.state.value.day!!.activitySteps ?: 0) shouldBeGreaterThan 0
        vm.state.value.day!!.totalBurnedKcal shouldBe vm.state.value.day!!.activityKcal
        vm.state.value.activitySource shouldBe ActivitySource.DeviceSensor
        vm.state.value.balance!!.restingKcal shouldBeGreaterThan 0
        vm.state.value.balance!!.burnedKcal shouldBe
            vm.state.value.balance!!.restingKcal + vm.state.value.balance!!.activityKcal
    }
}
