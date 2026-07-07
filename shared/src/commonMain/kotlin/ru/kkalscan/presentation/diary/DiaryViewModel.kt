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

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() {
        val date = diaryRepository.currentDate()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            coroutineScope {
                val dayDeferred = async { diaryRepository.getToday() }
                val hcAvailableDeferred = async { healthConnect.isAvailable() }
                val hcPermissionsDeferred = async { healthConnect.hasPermissions() }
                val hcKcalDeferred = async { healthConnect.readTodayActiveCalories() }
                val hcStepsDeferred = async { healthConnect.readTodaySteps() }
                val day = dayDeferred.await()
                val healthConnectKcal = hcKcalDeferred.await()
                DiaryLoadResult(
                    day = day,
                    balance = CalorieBalanceCalculator.compute(day, healthConnectKcal),
                    steps = hcStepsDeferred.await(),
                    healthConnectAvailable = hcAvailableDeferred.await(),
                    healthConnectPermissionsGranted = hcPermissionsDeferred.await(),
                )
            }
        }.onSuccess { result ->
            _state.update {
                DiaryUiState(
                    isLoading = false,
                    day = result.day,
                    balance = result.balance,
                    steps = result.steps,
                    date = date,
                    healthConnectAvailable = result.healthConnectAvailable,
                    healthConnectPermissionsGranted = result.healthConnectPermissionsGranted,
                )
            }
        }.onFailure { e ->
            kkalLog("Diary", "refresh fail ${e::class.simpleName}: ${e.message}")
            _state.update {
                it.copy(isLoading = false, date = date, errorMessage = e.userMessage())
            }
        }
    }

    override suspend fun onForeground() {
        refresh()
    }

    override suspend fun deleteEntry(entryId: String) {
        runCatching { diaryRepository.deleteEntry(entryId) }
            .onSuccess { refresh() }
            .onFailure { e ->
                kkalLog("Diary", "delete fail entryId=${entryId.take(8)}… ${e.message}")
                _state.update { it.copy(errorMessage = e.userMessage()) }
            }
    }

    override suspend fun addWorkout(name: String, kcal: Int) {
        _state.update { it.copy(errorMessage = null) }
        runCatching { diaryRepository.addWorkout(name, kcal) }
            .onSuccess { day -> updateDayState(day, clearWorkoutParse = true) }
            .onFailure { e ->
                kkalLog("Diary", "add workout fail ${e.message}")
                _state.update { it.copy(errorMessage = e.userMessage()) }
            }
    }

    override suspend fun parseWorkoutDescription(description: String) {
        _state.update {
            it.copy(
                workoutParse = WorkoutParseUiState(isLoading = true),
                errorMessage = null,
            )
        }
        runCatching { diaryRepository.parseWorkout(description) }
            .onSuccess { preview ->
                _state.update {
                    it.copy(
                        workoutParse = WorkoutParseUiState(
                            isLoading = false,
                            preview = preview,
                        ),
                    )
                }
            }
            .onFailure { e ->
                kkalLog("Diary", "parse workout fail ${e.message}")
                _state.update {
                    it.copy(
                        workoutParse = WorkoutParseUiState(
                            isLoading = false,
                            errorMessage = e.userMessage(isWorkoutParse = true),
                        ),
                    )
                }
            }
    }

    override suspend fun confirmParsedWorkout(): Boolean {
        val preview = _state.value.workoutParse.preview ?: return false
        _state.update {
            it.copy(workoutParse = it.workoutParse.copy(isLoading = true, errorMessage = null))
        }
        return runCatching { diaryRepository.addWorkout(preview.title, preview.burnedKcal) }
            .onSuccess { day -> updateDayState(day, clearWorkoutParse = true) }
            .onFailure { e ->
                kkalLog("Diary", "confirm workout fail ${e.message}")
                _state.update {
                    it.copy(
                        workoutParse = it.workoutParse.copy(
                            isLoading = false,
                            errorMessage = e.userMessage(),
                        ),
                    )
                }
            }
            .isSuccess
    }

    override fun clearWorkoutParse() {
        _state.update { it.copy(workoutParse = WorkoutParseUiState()) }
    }

    override suspend fun deleteWorkout(workoutId: String) {
        runCatching { diaryRepository.deleteWorkout(workoutId) }
            .onSuccess { refresh() }
            .onFailure { e ->
                kkalLog("Diary", "delete workout fail workoutId=${workoutId.take(8)}… ${e.message}")
                _state.update { it.copy(errorMessage = e.userMessage()) }
            }
    }

    override fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun updateDayState(day: DiaryDay, clearWorkoutParse: Boolean = false) {
        val healthConnectKcal = healthConnect.readTodayActiveCalories()
        _state.update {
            it.copy(
                isLoading = false,
                day = day,
                balance = CalorieBalanceCalculator.compute(day, healthConnectKcal),
                date = day.date,
                workoutParse = if (clearWorkoutParse) WorkoutParseUiState() else it.workoutParse,
            )
        }
    }

    private fun Throwable.userMessage(isWorkoutParse: Boolean = false): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.LimitHit -> message ?: "Лимит сканов исчерпан"
        is KkalScanException.Api -> message ?: if (isWorkoutParse) {
            "Не удалось понять описание тренировки"
        } else {
            "Ошибка сервера"
        }
        else -> message ?: "Неизвестная ошибка"
    }

    private data class DiaryLoadResult(
        val day: DiaryDay,
        val balance: ru.kkalscan.domain.activity.CalorieBalance,
        val steps: Int?,
        val healthConnectAvailable: Boolean,
        val healthConnectPermissionsGranted: Boolean,
    )
}
