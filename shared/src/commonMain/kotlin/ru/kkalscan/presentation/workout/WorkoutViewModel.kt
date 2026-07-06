package ru.kkalscan.presentation.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kkalscan.data.repository.IActivityRepository
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.util.kkalLog

class WorkoutViewModel(
    private val activityRepository: IActivityRepository,
    private val scope: CoroutineScope,
) : IWorkoutViewModel {

    private val _state = MutableStateFlow(WorkoutUiState())
    override val state: StateFlow<WorkoutUiState> = _state.asStateFlow()

    override suspend fun describeText(description: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        kkalLog("Workout", "describe start chars=${description.trim().length}")
        runCatching { activityRepository.describeWorkout(description) }
            .onSuccess { result ->
                kkalLog("Workout", "describe ok name=${result.name} kcal=${result.kcal}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                        descriptionText = description.trim(),
                    )
                }
            }
            .onFailure { e ->
                kkalLog("Workout", "describe fail ${e::class.simpleName}: ${e.message}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.userMessage(),
                    )
                }
            }
    }

    override suspend fun addToDay(): Result<Unit> {
        val result = _state.value.result ?: return Result.failure(IllegalStateException("No workout"))
        val description = _state.value.descriptionText
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        return runCatching {
            activityRepository.addWorkout(result, description)
        }
            .onSuccess {
                kkalLog("Workout", "added name=${result.name} kcal=${result.kcal}")
                _state.update { it.copy(isSaving = false) }
            }
            .map { }
            .onFailure { e ->
                kkalLog("Workout", "add fail ${e.message}")
                _state.update { it.copy(isSaving = false, errorMessage = e.userMessage()) }
            }
    }

    override fun reset() {
        _state.value = WorkoutUiState()
    }

    override fun launchAddToDay(onSuccess: () -> Unit) {
        scope.launch {
            if (addToDay().isSuccess) {
                onSuccess()
            }
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.Api -> message ?: "Не удалось понять описание тренировки"
        else -> message ?: "Ошибка"
    }
}
