package ru.kkalscan.presentation.diary

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kkalscan.data.health.IHealthConnectReader
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.domain.activity.CalorieBalanceCalculator
import ru.kkalscan.domain.error.KkalScanException

class DiaryViewModel(
    private val diaryRepository: IDiaryRepository,
    private val healthConnect: IHealthConnectReader,
    private val scope: CoroutineScope,
) : IDiaryViewModel {
    private val _state = MutableStateFlow(DiaryUiState(isLoading = true))
    override val state: StateFlow<DiaryUiState> = _state.asStateFlow()
    init { scope.launch { refresh() } }

    override suspend fun refresh() {
        val date = diaryRepository.currentDate()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            coroutineScope {
                val day = async { diaryRepository.getToday() }
                val hcAvail = async { healthConnect.isAvailable() }
                val hcPerm = async { healthConnect.hasPermissions() }
                val hcKcal = async { healthConnect.readTodayActiveCalories() }
                val hcSteps = async { healthConnect.readTodaySteps() }
                val d = day.await()
                DiaryLoadResult(d, CalorieBalanceCalculator.compute(d, hcKcal.await()), hcSteps.await(), hcAvail.await(), hcPerm.await())
            }
        }.onSuccess { r -> _state.update { DiaryUiState(false, r.day, r.balance, r.steps, date, null, r.healthConnectAvailable, r.healthConnectPermissionsGranted) } }
         .onFailure { e -> _state.update { it.copy(isLoading = false, date = date, errorMessage = e.userMessage()) } }
    }

    override suspend fun onForeground() {
        val loaded = _state.value.date ?: return
        if (loaded != diaryRepository.currentDate()) refresh()
    }

    override suspend fun deleteEntry(entryId: String) { runCatching { diaryRepository.deleteEntry(entryId) }.onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } } }
    override suspend fun addWorkout(name: String, kcal: Int) { runCatching { diaryRepository.addWorkout(name, kcal) }.onSuccess { updateDayState(it) }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } } }
    override suspend fun deleteWorkout(workoutId: String) { runCatching { diaryRepository.deleteWorkout(workoutId) }.onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } } }
    override fun clearError() { _state.update { it.copy(errorMessage = null) } }

    private suspend fun updateDayState(day: ru.kkalscan.domain.model.DiaryDay) {
        _state.update { it.copy(isLoading = false, day = day, balance = CalorieBalanceCalculator.compute(day, healthConnect.readTodayActiveCalories()), date = day.date) }
    }
    private fun Throwable.userMessage() = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.LimitHit -> message ?: "Лимит сканов исчерпан"
        is KkalScanException.Api -> message ?: "Ошибка сервера"
        else -> message ?: "Неизвестная ошибка"
    }
    private data class DiaryLoadResult(val day: ru.kkalscan.domain.model.DiaryDay, val balance: ru.kkalscan.domain.activity.CalorieBalance, val steps: Int?, val healthConnectAvailable: Boolean, val healthConnectPermissionsGranted: Boolean)
}
