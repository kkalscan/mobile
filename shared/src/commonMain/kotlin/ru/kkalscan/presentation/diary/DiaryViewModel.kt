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
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.util.kkalLog

class DiaryViewModel(
    private val diaryRepository: IDiaryRepository,
    private val healthConnect: IHealthConnectReader,
    private val scope: CoroutineScope,
) : IDiaryViewModel {
    private val _state = MutableStateFlow(DiaryUiState(isLoading = true))
    override val state: StateFlow<DiaryUiState> = _state.asStateFlow()
    /** Bumped on every local diary mutation so in-flight refresh() cannot overwrite newer state. */
    private var dataGeneration = 0
    init { scope.launch { refresh() } }

    override suspend fun refresh() {
        val generation = ++dataGeneration
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
                val kcal = hcKcal.await()
                DiaryLoadResult(d, CalorieBalanceCalculator.compute(d, kcal), hcSteps.await(), hcAvail.await(), hcPerm.await())
            }
        }.onSuccess { r ->
            if (generation != dataGeneration) return@onSuccess
            _state.update { it.copy(isLoading = false, day = r.day, balance = r.balance, steps = r.steps, date = date, healthConnectAvailable = r.healthConnectAvailable, healthConnectPermissionsGranted = r.healthConnectPermissionsGranted) }
        }.onFailure { e ->
            if (generation != dataGeneration) return@onFailure
            _state.update { it.copy(isLoading = false, date = date, errorMessage = e.userMessage()) }
        }
    }

    override suspend fun onForeground() {
        val loaded = _state.value.date ?: return
        if (loaded != diaryRepository.currentDate()) refresh()
    }

    override suspend fun deleteEntry(entryId: String) {
        runCatching { diaryRepository.deleteEntry(entryId) }.onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } }
    }

    override suspend fun addWorkout(name: String, kcal: Int) {
        _state.update { it.copy(errorMessage = null) }
        runCatching { diaryRepository.addWorkout(name, kcal) }
            .onSuccess {
                kkalLog("Diary", "workout saved kcal=$kcal, refreshing day")
                refresh()
            }
            .onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } }
    }

    override suspend fun parseWorkoutDescription(description: String) {
        _state.update { it.copy(workoutParse = WorkoutParseUiState(isLoading = true), errorMessage = null) }
        runCatching { diaryRepository.parseWorkout(description) }
            .onSuccess { preview -> _state.update { it.copy(workoutParse = WorkoutParseUiState(isLoading = false, preview = preview)) } }
            .onFailure { e -> _state.update { it.copy(workoutParse = WorkoutParseUiState(isLoading = false, errorMessage = e.userMessage(true))) } }
    }

    override suspend fun confirmParsedWorkout(): Boolean {
        val preview = _state.value.workoutParse.preview ?: return false
        _state.update { it.copy(workoutParse = it.workoutParse.copy(isLoading = true, errorMessage = null)) }
        val saved = runCatching { diaryRepository.addWorkout(preview.title, preview.burnedKcal) }
            .onFailure { e -> _state.update { it.copy(workoutParse = it.workoutParse.copy(isLoading = false, errorMessage = e.userMessage())) } }
            .isSuccess
        if (!saved) return false
        ++dataGeneration
        _state.update { it.copy(workoutParse = WorkoutParseUiState()) }
        kkalLog("Diary", "workout confirmed ${preview.title} ${preview.burnedKcal} kcal, refreshing day")
        refresh()
        return true
    }

    override fun clearWorkoutParse() { _state.update { it.copy(workoutParse = WorkoutParseUiState()) } }

    override suspend fun deleteWorkout(workoutId: String) {
        runCatching { diaryRepository.deleteWorkout(workoutId) }.onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } }
    }

    override fun clearError() { _state.update { it.copy(errorMessage = null) } }

    private suspend fun updateDayState(day: DiaryDay, clearWorkoutParse: Boolean = false) {
        ++dataGeneration
        val hcKcal = healthConnect.readTodayActiveCalories()
        _state.update { it.copy(isLoading = false, day = day, balance = CalorieBalanceCalculator.compute(day, hcKcal), date = day.date, workoutParse = if (clearWorkoutParse) WorkoutParseUiState() else it.workoutParse) }
    }

    private fun Throwable.userMessage(isWorkoutParse: Boolean = false) = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.LimitHit -> message ?: "Лимит сканов исчерпан"
        is KkalScanException.Api -> message ?: if (isWorkoutParse) "Не удалось понять описание тренировки" else "Ошибка сервера"
        else -> message ?: "Неизвестная ошибка"
    }

    private data class DiaryLoadResult(val day: DiaryDay, val balance: ru.kkalscan.domain.activity.CalorieBalance, val steps: Int?, val healthConnectAvailable: Boolean, val healthConnectPermissionsGranted: Boolean)
}
