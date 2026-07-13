package ru.kkalscan.presentation.journal

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.kkalscan.FailingDiaryApi
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.data.repository.IInsightRepository
import ru.kkalscan.insights.DietitianInsight
import ru.kkalscan.offlineDiaryRepo
import ru.kkalscan.sampleCachedDay
import ru.kkalscan.stats.WeekDates
import ru.kkalscan.stats.WeekStats
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelOfflineTest {

    @Test
    fun refresh_offlineWithCachedWeek_showsStats() = runTest {
        val weekStart = WeekDates.currentWeekStart()
        val dates = WeekDates.weekFrom(WeekDates.parse(weekStart))
        val store = InMemoryDiaryLocalStore()
        dates.forEachIndexed { index, date ->
            store.upsert(sampleCachedDay(date = date, totalKcal = 100 * (index + 1)))
        }
        val vm = JournalViewModel(
            offlineDiaryRepo(FailingDiaryApi(), store),
            FakeInsightRepository(),
            this,
            refreshOnInit = false,
        )
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.week.shouldNotBeNull()
            vm.state.value.week!!.daysWithData shouldBe 7
            vm.state.value.errorMessage.shouldBeNull()
            vm.state.value.isLoading shouldBe false
        } finally {
            vm.tearDownForTest()
        }
    }

    @Test
    fun refresh_offlineWithoutCache_showsError() = runTest {
        val vm = JournalViewModel(
            offlineDiaryRepo(FailingDiaryApi()),
            FakeInsightRepository(),
            this,
            refreshOnInit = false,
        )
        try {
            vm.refresh()
            advanceUntilIdle()

            vm.state.value.week.shouldBeNull()
            vm.state.value.errorMessage.shouldNotBeNull()
        } finally {
            vm.tearDownForTest()
        }
    }
}

private class FakeInsightRepository : IInsightRepository {
    override suspend fun requestDietitianInsight(weekStart: String, week: WeekStats): DietitianInsight =
        DietitianInsight(
            weekStart = weekStart,
            generatedAt = weekStart,
            headline = "ok",
            sections = emptyList(),
        )
}
