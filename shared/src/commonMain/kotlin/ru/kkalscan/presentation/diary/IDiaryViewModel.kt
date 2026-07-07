package ru.kkalscan.presentation.diary

import ru.kkalscan.domain.activity.CalorieBalance
import ru.kkalscan.domain.model.DiaryDay

data class DiaryUiState(
    val isLoading: Boolean = false,
    val day: DiaryDay? = null,
    val balance: CalorieBalance? = null,
    val steps: Int? = null,
    val date: String? = null,
    val errorMessage: String? = null,
    val healthConnectAvailable: Boolean = false,
    val healthConnectPermissionsGranted: Boolean = false,
)

interface IDiaryViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<DiaryUiState>
    suspend fun refresh()
    suspend fun onForeground()
    suspend fun deleteEntry(entryId: String)
    suspend fun addWorkout(name: String, kcal: Int)
    suspend fun deleteWorkout(workoutId: String)
    fun clearError()
}
