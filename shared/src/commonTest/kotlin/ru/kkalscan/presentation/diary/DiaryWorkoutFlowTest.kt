package ru.kkalscan.presentation.diary

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.StatefulDiaryApi
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.domain.model.DiaryDay
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryWorkoutFlowTest {

    @Test
    fun addWorkout_updatesBalanceOnDiary() = runTest {
        val api = StatefulDiaryApi(diaryDate = TestApiFixtures.TODAY)
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = DiaryRepository(api, storage, todayProvider = { TestApiFixtures.TODAY })
        val vm = createDiaryViewModelForTest(repo, this, api, storage)
        advanceUntilIdle()

        vm.addWorkout("Бег", 250)
        advanceUntilIdle()

        vm.state.value.day!!.totalBurnedKcal shouldBe 250
        vm.state.value.day!!.netKcal shouldBe -250
        vm.state.value.day!!.workouts.single().name shouldBe "Бег"
    }

    @Test
    fun parseWorkoutDescription_confirm_addsWorkoutToDiary() = runTest {
        val api = FakeKkalScanApi(todayProvider = { TestApiFixtures.TODAY })
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = DiaryRepository(api, storage, todayProvider = { TestApiFixtures.TODAY })
        val vm = createDiaryViewModelForTest(repo, this, api, storage)
        advanceUntilIdle()

        vm.parseWorkoutDescription("бег 30 минут")
        advanceUntilIdle()

        val preview = vm.state.value.workoutParse.preview.shouldNotBeNull()
        preview.title shouldBe "Бег"
        preview.burnedKcal shouldBe 300
        preview.durationMinutes shouldBe 30

        vm.confirmParsedWorkout() shouldBe true
        advanceUntilIdle()

        vm.state.value.day!!.totalBurnedKcal shouldBe 300
        vm.state.value.day!!.workouts.single().name shouldBe "Бег"
        vm.state.value.workoutParse.preview shouldBe null
    }

    @Test
    fun confirmParsedWorkout_staleInitRefresh_doesNotOverwriteWorkout() = runTest {
        val releaseInitRefresh = CompletableDeferred<Unit>()
        val api = StatefulDiaryApi(diaryDate = TestApiFixtures.TODAY)
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = GatedGetTodayRepository(
            DiaryRepository(api, storage, todayProvider = { TestApiFixtures.TODAY }),
            releaseInitRefresh,
        )
        val vm = createDiaryViewModelForTest(repo, this, api, storage)

        vm.parseWorkoutDescription("бег 30 минут")
        advanceUntilIdle()

        vm.confirmParsedWorkout() shouldBe true

        releaseInitRefresh.complete(Unit)
        advanceUntilIdle()

        vm.state.value.day!!.totalBurnedKcal shouldBe 300
        vm.state.value.balance!!.workoutKcal shouldBe 300
        vm.state.value.balance!!.burnedKcal shouldBe 300 + vm.state.value.balance!!.activityKcal
    }
}

/** Delays the first getToday() and returns the snapshot from when it started (stale after add). */
private class GatedGetTodayRepository(
    private val delegate: IDiaryRepository,
    private val releaseFirstGetToday: CompletableDeferred<Unit>,
) : IDiaryRepository by delegate {
    private var gateUsed = false

    override suspend fun getToday(timezoneOffsetMinutes: Int): DiaryDay {
        if (!gateUsed) {
            gateUsed = true
            val staleDay = delegate.getToday(timezoneOffsetMinutes)
            releaseFirstGetToday.await()
            return staleDay
        }
        return delegate.getToday(timezoneOffsetMinutes)
    }
}
