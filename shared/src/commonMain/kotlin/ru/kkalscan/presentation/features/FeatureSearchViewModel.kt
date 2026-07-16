package ru.kkalscan.presentation.features

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kkalscan.data.repository.IFeatureSearchRepository
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.FeatureSearchItem
import ru.kkalscan.util.kkalLog

typealias FeatureSearchCompletedListener = (query: String, resultsCount: Int) -> Unit
typealias FeatureSearchFoodIntentAnalytics = (queryLength: Int, isFood: Boolean) -> Unit

data class FeatureSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<FeatureSearchItem> = emptyList(),
    val showPopular: Boolean = false,
    val errorMessage: String? = null,
)

interface IFeatureSearchViewModel {
    val state: StateFlow<FeatureSearchUiState>
    val foodIntentEvents: SharedFlow<Unit>
    fun onQueryChange(query: String)
    fun onSubmit()
    fun clear()
}

class FeatureSearchViewModel(
    private val featureSearchRepository: IFeatureSearchRepository,
    private val scope: CoroutineScope,
    private val onSearchCompleted: FeatureSearchCompletedListener = { _, _ -> },
    private val onFoodIntentAnalytics: FeatureSearchFoodIntentAnalytics = { _, _ -> },
) : IFeatureSearchViewModel {

    private val _state = MutableStateFlow(FeatureSearchUiState())
    override val state: StateFlow<FeatureSearchUiState> = _state.asStateFlow()
    private val _foodIntentEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val foodIntentEvents: SharedFlow<Unit> = _foodIntentEvents.asSharedFlow()
    private var searchJob: Job? = null

    override fun onQueryChange(query: String) {
        searchJob?.cancel()
        _state.update {
            it.copy(
                query = query,
                isSearching = false,
                errorMessage = null,
            )
        }
    }

    override fun onSubmit() {
        searchJob?.cancel()
        searchJob = scope.launch {
            val trimmed = _state.value.query.trim()
            _state.update { it.copy(isSearching = true, errorMessage = null) }
            if (trimmed.isBlank()) {
                _state.update {
                    it.copy(
                        isSearching = false,
                        results = emptyList(),
                        showPopular = false,
                    )
                }
                onSearchCompleted(trimmed, 0)
                return@launch
            }

            val searchResult = runCatching { featureSearchRepository.search(trimmed) }
            searchResult
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
                .onSuccess { result ->
                    val hasRealMatch = !result.popularFallback && result.items.isNotEmpty()
                    if (hasRealMatch) {
                        _state.update {
                            it.copy(
                                isSearching = false,
                                results = result.items,
                                showPopular = false,
                                errorMessage = null,
                            )
                        }
                        onSearchCompleted(trimmed, result.items.size)
                        return@launch
                    }

                    if (trimmed.length < MIN_INTENT_QUERY_CHARS) {
                        _state.update {
                            it.copy(
                                isSearching = false,
                                results = result.items,
                                showPopular = result.popularFallback,
                                errorMessage = null,
                            )
                        }
                        onSearchCompleted(trimmed, 0)
                        return@launch
                    }

                    val intent = runCatching { featureSearchRepository.classifyIntent(trimmed) }
                    intent
                        .onSuccess { classified ->
                            onFoodIntentAnalytics(trimmed.length, classified.isFoodIntent)
                            if (classified.isFoodIntent) {
                                _state.value = FeatureSearchUiState()
                                onSearchCompleted(trimmed, 0)
                                _foodIntentEvents.tryEmit(Unit)
                            } else {
                                _state.update {
                                    it.copy(
                                        isSearching = false,
                                        results = result.items,
                                        showPopular = result.popularFallback,
                                        errorMessage = null,
                                    )
                                }
                                onSearchCompleted(trimmed, 0)
                            }
                        }
                        .onFailure { e ->
                            kkalLog("FeatureSearch", "intent fail ${e.message}")
                            onFoodIntentAnalytics(trimmed.length, false)
                            _state.update {
                                it.copy(
                                    isSearching = false,
                                    results = result.items,
                                    showPopular = result.popularFallback,
                                    errorMessage = null,
                                )
                            }
                            onSearchCompleted(trimmed, 0)
                        }
                }
        }
    }

    override fun clear() {
        searchJob?.cancel()
        _state.value = FeatureSearchUiState()
    }

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.Api -> message ?: "Ошибка сервера"
        else -> message ?: "Неизвестная ошибка"
    }

    private companion object {
        const val MIN_INTENT_QUERY_CHARS = 3
    }
}
