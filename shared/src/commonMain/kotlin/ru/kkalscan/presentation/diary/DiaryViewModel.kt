package ru.kkalscan.presentation.diary

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.util.kkalLog

class DiaryViewModel(
    private val diaryRepository: IDiaryRepository,
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
        runCatching { diaryRepository.getToday() }
            .onSuccess { day -> _state.update { DiaryUiState(isLoading = false, day = day, date = date) } }
            .onFailure { e ->
                kkalLog("Diary", "refresh fail ${e::class.simpleName}: ${e.message}")
                _state.update {
                    it.copy(isLoading = false, date = date, errorMessage = e.userMessage())
                }
            }
    }

    override suspend fun onForeground() {
        val loadedDate = _state.value.date ?: return
        val currentDate = diaryRepository.currentDate()
        if (loadedDate != currentDate) {
            kkalLog("Diary", "date rolled over $loadedDate -> $currentDate, refreshing")
            refresh()
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        runCatching { diaryRepository.deleteEntry(entryId) }
            .onSuccess { refresh() }
            .onFailure { e ->
                kkalLog("Diary", "delete fail entryId=${entryId.take(8)}… ${e.message}")
                _state.update { it.copy(errorMessage = e.userMessage()) }
            }
    }

    override fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.LimitHit -> message ?: "Лимит сканов исчерпан"
        is KkalScanException.Api -> message ?: "Ошибка сервера"
        else -> message ?: "Неизвестная ошибка"
    }
}
