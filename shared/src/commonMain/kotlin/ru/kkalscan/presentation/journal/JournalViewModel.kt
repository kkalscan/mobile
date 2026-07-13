package ru.kkalscan.presentation.journal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.repository.IInsightRepository
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.insights.DietitianInsight
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.stats.JournalDayMerger
import ru.kkalscan.stats.StatsAggregator
import ru.kkalscan.stats.WeekDates
import ru.kkalscan.stats.WeekStats

data class JournalUiState(
    val weekStart: String = WeekDates.currentWeekStart(),
    val isLoading: Boolean = true,
    val week: WeekStats? = null,
    val errorMessage: String? = null,
    val insightLoading: Boolean = false,
    val insight: DietitianInsight? = null,
    val insightError: String? = null,
)

interface IJournalViewModel {
    val state: StateFlow<JournalUiState>
    suspend fun refresh()
    fun previousWeek()
    fun nextWeek()
    suspend fun requestDietitianInsight(): InsightRequestResult
    fun clearInsight()
    fun clearInsightError()
}

sealed class InsightRequestResult {
    data object Success : InsightRequestResult()
    data object NeedPro : InsightRequestResult()
    data class Error(val message: String) : InsightRequestResult()
}

class JournalViewModel(
    private val diaryRepository: IDiaryRepository,
    private val insightRepository: IInsightRepository,
    private val scope: CoroutineScope,
    private val todayPatchProvider: () -> DiaryDay? = { null },
    private val refreshOnInit: Boolean = true,
) : IJournalViewModel {

    private val _state = MutableStateFlow(JournalUiState())
    override val state: StateFlow<JournalUiState> = _state.asStateFlow()
    private var weekObserveJob: Job? = null
    private var refreshJob: Job? = null

    init {
        if (refreshOnInit) scope.launch { refresh() }
    }

    override suspend fun refresh() {
        refreshJob?.cancelAndJoin()
        coroutineScope {
            refreshJob = coroutineContext[Job]
            refreshBody()
        }
    }

    private suspend fun refreshBody() {
        val weekStart = _state.value.weekStart
        weekObserveJob?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val firstReady = kotlinx.coroutines.CompletableDeferred<Unit>()
        weekObserveJob = scope.launch {
            val dates = WeekDates.weekFrom(WeekDates.parse(weekStart))
            val flows = dates.map { date -> diaryRepository.observeDay(date) }
            var signaled = false
            combine(flows) { resources -> resources.toList() }.collect { resources ->
                val days = resources.mapNotNull { it.day }
                val refreshing = resources.any { it.isRefreshing }
                val firstError = resources.firstOrNull { it.error != null && it.day == null }?.error
                if (days.size == dates.size) {
                    val merged = JournalDayMerger.mergeWeekWithTodayPatch(days, todayPatchProvider())
                    _state.update {
                        it.copy(
                            isLoading = false,
                            week = StatsAggregator.weekStats(merged, weekStart),
                            errorMessage = null,
                        )
                    }
                } else if (!refreshing && firstError != null) {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = firstError.userMessage())
                    }
                } else {
                    _state.update { it.copy(isLoading = days.isEmpty() && refreshing) }
                }
                if (!signaled && (days.size == dates.size || (!refreshing && firstError != null))) {
                    signaled = true
                    firstReady.complete(Unit)
                }
            }
        }
        firstReady.await()
    }

    override fun previousWeek() {
        val monday = WeekDates.parse(_state.value.weekStart)
        val prev = WeekDates.iso(monday.minus(DatePeriod(days = 7)))
        _state.update { it.copy(weekStart = prev, insight = null) }
        scope.launch { refresh() }
    }

    override fun nextWeek() {
        val monday = WeekDates.parse(_state.value.weekStart)
        val next = WeekDates.iso(monday.plus(DatePeriod(days = 7)))
        val currentStart = WeekDates.mondayOf(WeekDates.today())
        if (WeekDates.isAfter(WeekDates.parse(next), currentStart)) return
        _state.update { it.copy(weekStart = next, insight = null) }
        scope.launch { refresh() }
    }

    override suspend fun requestDietitianInsight(): InsightRequestResult {
        val week = _state.value.week ?: return InsightRequestResult.Error("Нет данных")
        if (!week.isPro) return InsightRequestResult.NeedPro
        _state.update { it.copy(insightLoading = true, insightError = null) }
        return runCatching { insightRepository.requestDietitianInsight(_state.value.weekStart, week) }
            .fold(
                onSuccess = { insight ->
                    _state.update { it.copy(insightLoading = false, insight = insight) }
                    InsightRequestResult.Success
                },
                onFailure = { e ->
                    val msg = e.userMessage()
                    _state.update { it.copy(insightLoading = false, insightError = msg) }
                    InsightRequestResult.Error(msg)
                },
            )
    }

    override fun clearInsight() {
        _state.update { it.copy(insight = null) }
    }

    override fun clearInsightError() {
        _state.update { it.copy(insightError = null) }
    }

    /** Stops week Flow collectors; used from unit tests to avoid leaking jobs in [runTest]. */
    internal fun tearDownForTest() {
        refreshJob?.cancel()
        refreshJob = null
        weekObserveJob?.cancel()
        weekObserveJob = null
    }

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.Api -> message ?: "Ошибка"
        else -> message ?: "Неизвестная ошибка"
    }
}
