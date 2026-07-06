package ru.kkalscan.presentation.workout

import ru.kkalscan.domain.model.WorkoutResult

data class WorkoutUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val result: WorkoutResult? = null,
    val descriptionText: String? = null,
    val errorMessage: String? = null,
)

interface IWorkoutViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<WorkoutUiState>
    suspend fun describeText(description: String)
    suspend fun addToDay(): Result<Unit>
    fun reset()
    fun launchAddToDay(onSuccess: () -> Unit = {})
}
