package ru.kkalscan.presentation.workout

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.health.NoOpHealthConnectReader
import ru.kkalscan.data.repository.ActivityRepository
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.data.storage.InMemoryWorkoutStorage
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.data.repository.ScanRepository
import ru.kkalscan.presentation.diary.createDiaryViewModelForTest
import ru.kkalscan.presentation.scan.ScanViewModel
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DescribeWorkoutFlowTest {

    private val today = "2026-07-04"

    @Test
    fun describeWorkout_addToDay_persistsWorkout() = runTest {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId("11111111-1111-1111-1111-111111111111") }
        val api = FakeKkalScanApi(todayProvider = { today })
        val workoutStorage = InMemoryWorkoutStorage()
        val activityRepo = ActivityRepository(
            api = api,
            deviceIdStorage = storage,
            healthConnect = NoOpHealthConnectReader(),
            workoutStorage = workoutStorage,
            todayProvider = { today },
        )
        val workoutVm = WorkoutViewModel(activityRepo, this)

        workoutVm.describeText("бег 5 км")
        advanceUntilIdle()

        workoutVm.state.value.result.shouldNotBeNull()
        workoutVm.state.value.result!!.name shouldBe "Бег"
        workoutVm.state.value.result!!.kcal shouldBe 280

        workoutVm.addToDay().isSuccess shouldBe true
        advanceUntilIdle()

        val activity = activityRepo.getToday()
        activity.workouts shouldHaveSize 1
        activity.workoutKcal shouldBe 280
    }

    @Test
    fun foodAndWorkout_diaryShowsCalorieDeficit() = runTest {
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId("11111111-1111-1111-1111-111111111111") }
        val api = FakeKkalScanApi(todayProvider = { today })
        val workoutStorage = InMemoryWorkoutStorage()
        val diaryRepo = DiaryRepository(api, storage, todayProvider = { today })
        val scanRepo = ScanRepository(api, storage)
        val activityRepo = ActivityRepository(
            api = api,
            deviceIdStorage = storage,
            healthConnect = NoOpHealthConnectReader(activeCalories = 300, available = true, permissionsGranted = true),
            workoutStorage = workoutStorage,
            todayProvider = { today },
        )
        val scanVm = ScanViewModel(scanRepo, diaryRepo, this)
        val workoutVm = WorkoutViewModel(activityRepo, this)
        val diaryVm = createDiaryViewModelForTest(
            diaryRepository = diaryRepo,
            scope = this,
            api = api,
            healthConnect = NoOpHealthConnectReader(activeCalories = 300, available = true, permissionsGranted = true),
            workoutStorage = workoutStorage,
            todayProvider = { today },
        )
        advanceUntilIdle()

        scanVm.describeText("тарелка борща")
        advanceUntilIdle()
        scanVm.selectMealType(MealType.lunch)
        scanVm.addToDiary().isSuccess shouldBe true

        workoutVm.describeText("бег 30 минут")
        advanceUntilIdle()
        workoutVm.addToDay().isSuccess shouldBe true

        diaryVm.refresh()
        advanceUntilIdle()

        val balance = diaryVm.state.value.balance.shouldNotBeNull()
        balance.eatenKcal shouldBe 250
        balance.healthConnectKcal shouldBe 300
        balance.workoutKcal shouldBe 280
        balance.burnedKcal shouldBe 580
        balance.deficitKcal shouldBe 330
        balance.isDeficit shouldBe true
    }
}
