package ru.kkalscan.presentation.journal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.repository.IInsightRepository
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.insights.DietitianInsight
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
) : IJournalViewModel {

    private val _state = MutableStateFlow(JournalUiState())
    override val state: StateFlow<JournalUiState> = _state.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() {
        val weekStart = _state.value.weekStart
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { diaryRepository.getWeek(weekStart) }
            .onSuccess { days ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        week = StatsAggregator.weekStats(days, weekStart),
                    )
                }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(isLoading = false, errorMessage = e.userMessage())
                }
            }
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

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.Api -> message ?: "Ошибка"
        else -> message ?: "Неизвестная ошибка"
    }
}
