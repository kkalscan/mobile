package ru.kkalscan.presentation.diary

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.StatefulDiaryApi
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryWorkoutFlowTest {

    @Test
    fun addWorkout_updatesBalanceOnDiary() = runTest {
        val api = StatefulDiaryApi(diaryDate = TestApiFixtures.TODAY)
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = DiaryRepository(api, storage, todayProvider = { TestApiFixtures.TODAY })
        val vm = createDiaryViewModelForTest(repo, this)
        advanceUntilIdle()

        vm.addWorkout("Бег", 250)
        advanceUntilIdle()

        vm.state.value.day!!.totalBurnedKcal shouldBe 250
        vm.state.value.day!!.netKcal shouldBe -250
        vm.state.value.day!!.workouts.single().name shouldBe "Бег"
        vm.state.value.balance.shouldNotBeNull()
        vm.state.value.balance!!.workoutKcal shouldBe 250
    }
}
