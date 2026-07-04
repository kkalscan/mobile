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
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.MealType
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {

    @Test
    fun refresh_loadsDiary() = runTest {
        val repo = DiaryRepository(
            TestApiFixtures.api(),
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            { TestApiFixtures.TODAY },
        )
        val vm = DiaryViewModel(repo, this)
        vm.refresh()

        vm.state.value.isLoading shouldBe false
        vm.state.value.day.shouldNotBeNull()
        vm.state.value.day!!.totalKcal shouldBe 350
    }

    @Test
    fun deleteEntry_triggersRefresh() = runTest {
        val repo = DiaryRepository(
            TestApiFixtures.api(),
            InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) },
            { TestApiFixtures.TODAY },
        )
        val vm = DiaryViewModel(repo, this)
        vm.refresh()
        vm.deleteEntry("entry-1")

        vm.state.value.errorMessage shouldBe null
    }

    /**
     * Repro of the reported bug: launch app, background it, a full day passes,
     * then reopen. Before the fix the diary stayed on the previous day until the
     * app was restarted. onForeground() must roll it over to the new "today".
     */
    @Test
    fun onForeground_afterOvernightBackground_rollsDiaryOverToNewDay() = runTest {
        var today = "2026-07-03"
        val api = StatefulDiaryApi(diaryDate = "2026-07-03")
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = CountingDiaryRepository(
            DiaryRepository(api, storage, todayProvider = { today }),
        )
        val vm = DiaryViewModel(repo, this)
        advanceUntilIdle() // init refresh loads day one

        // Day one: the user logged a meal today.
        val scan = api.scanPhoto(storage.getDeviceId(), byteArrayOf(1), 0)
        repo.addFromScan(scan.scanId, MealType.lunch, scan.dishes)
        vm.refresh()
        advanceUntilIdle()

        vm.state.value.date shouldBe "2026-07-03"
        vm.state.value.day!!.date shouldBe "2026-07-03"
        vm.state.value.day!!.entries shouldHaveSize 1

        val fetchesBefore = repo.getTodayCalls

        // App sits in the background overnight; the calendar day advances.
        today = "2026-07-04"

        // Returning to the foreground must reload the diary for the new day.
        vm.onForeground()
        advanceUntilIdle()

        repo.getTodayCalls shouldBe fetchesBefore + 1
        vm.state.value.date shouldBe "2026-07-04"
        vm.state.value.day!!.date shouldBe "2026-07-04"
        vm.state.value.day!!.entries shouldHaveSize 0
    }

    /** Coming back the same day must not trigger a needless network reload. */
    @Test
    fun onForeground_sameDay_doesNotRefetch() = runTest {
        val today = "2026-07-03"
        val api = StatefulDiaryApi(diaryDate = today)
        val storage = InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }
        val repo = CountingDiaryRepository(
            DiaryRepository(api, storage, todayProvider = { today }),
        )
        val vm = DiaryViewModel(repo, this)
        advanceUntilIdle()

        val fetchesBefore = repo.getTodayCalls
        vm.onForeground()
        advanceUntilIdle()

        repo.getTodayCalls shouldBe fetchesBefore
    }
}

/** Wraps a real repository and counts how many times the diary is re-fetched. */
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
