package ru.kkalscan.presentation.diary

import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry

data class DiaryUiState(
    val isLoading: Boolean = false,
    val day: DiaryDay? = null,
    /** ISO date (yyyy-MM-dd) the currently shown diary was loaded for. */
    val date: String? = null,
    val errorMessage: String? = null,
)

interface IDiaryViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<DiaryUiState>
    suspend fun refresh()

    /**
     * Called when the app returns to the foreground. If the calendar day has
     * changed since the diary was loaded (e.g. the app spent the night in the
     * background), the diary is reloaded for the new "today".
     */
    suspend fun onForeground()
    suspend fun deleteEntry(entryId: String)
    suspend fun addWorkout(name: String, kcal: Int)
    suspend fun deleteWorkout(workoutId: String)
    fun clearError()
}
