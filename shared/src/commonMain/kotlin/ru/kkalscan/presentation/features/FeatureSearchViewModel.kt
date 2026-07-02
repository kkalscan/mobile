package ru.kkalscan.presentation.features

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ru.kkalscan.data.repository.IFeatureSearchRepository
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.FeatureSearchItem
import ru.kkalscan.util.kkalLog

typealias FeatureSearchCompletedListener = (query: String, resultsCount: Int) -> Unit

data class FeatureSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<FeatureSearchItem> = emptyList(),
    val showPopular: Boolean = false,
    val errorMessage: String? = null,
)

interface IFeatureSearchViewModel {
    val state: StateFlow<FeatureSearchUiState>
    fun onQueryChange(query: String)
    fun clear()
}

class FeatureSearchViewModel(
    private val featureSearchRepository: IFeatureSearchRepository,
    private val scope: CoroutineScope,
    private val onSearchCompleted: FeatureSearchCompletedListener = { _, _ -> },
) : IFeatureSearchViewModel {

    private val _state = MutableStateFlow(FeatureSearchUiState())
    override val state: StateFlow<FeatureSearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    override fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, errorMessage = null) }
        searchJob?.cancel()
        searchJob = scope.launch {
            val startedAtMs = Clock.System.now().toEpochMilliseconds()
            _state.update { it.copy(isSearching = true) }
            delay(SEARCH_DEBOUNCE_MS)
            val trimmed = query.trim()
            val result = runCatching { featureSearchRepository.search(trimmed) }
            ensureMinLoadingVisible(startedAtMs)
            result
                .onSuccess { searchResult ->
                    _state.update {
                        it.copy(
                            isSearching = false,
                            results = searchResult.items,
                            showPopular = searchResult.popularFallback,
                            errorMessage = null,
                        )
                    }
                    val matchedCount = if (searchResult.popularFallback) 0 else searchResult.items.size
                    onSearchCompleted(trimmed, matchedCount)
                }
                .onFailure { e ->
                    kkalLog("FeatureSearch", "search fail ${e.message}")
                    _state.update {
                        it.copy(
                            isSearching = false,
                            results = emptyList(),
                            showPopular = false,
                            errorMessage = e.userMessage(),
                        )
                    }
                    onSearchCompleted(trimmed, 0)
                }
        }
    }

    override fun clear() {
        searchJob?.cancel()
        _state.value = FeatureSearchUiState()
    }

    private suspend fun ensureMinLoadingVisible(startedAtMs: Long) {
        val elapsed = Clock.System.now().toEpochMilliseconds() - startedAtMs
        val minTotal = SEARCH_DEBOUNCE_MS + MIN_LOADING_VISIBLE_MS
        if (elapsed < minTotal) {
            delay(minTotal - elapsed)
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.Api -> message ?: "Ошибка сервера"
        else -> message ?: "Неизвестная ошибка"
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 900L
        const val MIN_LOADING_VISIBLE_MS = 500L
    }
}
