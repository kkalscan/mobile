package ru.kkalscan.presentation.food

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.repository.IFoodSearchRepository
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.onboarding.FirstLogTracker
import ru.kkalscan.onboarding.InMemoryHasLoggedAnythingStorage
import ru.kkalscan.presentation.scan.defaultMealType
import ru.kkalscan.util.kkalLog

data class FoodSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Dish> = emptyList(),
    val errorMessage: String? = null,
    val isAdding: Boolean = false,
    val selectedMealType: MealType = defaultMealType(),
    val addSuccess: Boolean = false,
)

interface IFoodSearchViewModel {
    val state: StateFlow<FoodSearchUiState>
    fun onQueryChange(query: String)
    fun selectMealType(mealType: MealType)
    suspend fun addDish(dish: Dish): Result<Unit>
    fun clear()
    fun consumeAddSuccess()
    fun launchAddFirstResult()
}

class FoodSearchViewModel(
    private val foodSearchRepository: IFoodSearchRepository,
    private val diaryRepository: IDiaryRepository,
    private val scope: CoroutineScope,
    private val firstLogTracker: FirstLogTracker = FirstLogTracker(InMemoryHasLoggedAnythingStorage()),
) : IFoodSearchViewModel {

    private val _state = MutableStateFlow(FoodSearchUiState())
    override val state: StateFlow<FoodSearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    override fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, errorMessage = null, addSuccess = false) }
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _state.update { it.copy(isSearching = false, results = emptyList()) }
            return
        }
        searchJob = scope.launch {
            _state.update { it.copy(isSearching = true) }
            delay(SEARCH_DEBOUNCE_MS)
            runCatching { foodSearchRepository.search(trimmed) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(isSearching = false, results = result.items, errorMessage = null)
                    }
                }
                .onFailure { e ->
                    kkalLog("FoodSearch", "search fail ${e.message}")
                    _state.update {
                        it.copy(isSearching = false, results = emptyList(), errorMessage = e.userMessage())
                    }
                }
        }
    }

    override fun selectMealType(mealType: MealType) {
        _state.update { it.copy(selectedMealType = mealType) }
    }

    override suspend fun addDish(dish: Dish): Result<Unit> =
        runCatching {
            _state.update { it.copy(isAdding = true, errorMessage = null) }
            diaryRepository.addFromDishes(listOf(dish), _state.value.selectedMealType)
            firstLogTracker.onFoodOrWorkoutLogged()
            _state.update { it.copy(isAdding = false, addSuccess = true) }
        }.onFailure { e ->
            kkalLog("FoodSearch", "add fail ${e.message}")
            _state.update { it.copy(isAdding = false, errorMessage = e.userMessage()) }
        }.map { }

    override fun clear() {
        searchJob?.cancel()
        _state.value = FoodSearchUiState(selectedMealType = _state.value.selectedMealType)
    }

    override fun consumeAddSuccess() {
        _state.update { it.copy(addSuccess = false) }
    }

    override fun launchAddFirstResult() {
        scope.launch {
            val dish = _state.value.results.firstOrNull() ?: return@launch
            addDish(dish)
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.Api -> message ?: "Ошибка сервера"
        else -> message ?: "Неизвестная ошибка"
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
