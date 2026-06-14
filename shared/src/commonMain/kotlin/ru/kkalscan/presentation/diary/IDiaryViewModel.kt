package ru.kkalscan.presentation.diary

import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry

data class DiaryUiState(
    val isLoading: Boolean = false,
    val day: DiaryDay? = null,
    val errorMessage: String? = null,
)

interface IDiaryViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<DiaryUiState>
    suspend fun refresh()
    suspend fun deleteEntry(entryId: String)
    fun clearError()
}
