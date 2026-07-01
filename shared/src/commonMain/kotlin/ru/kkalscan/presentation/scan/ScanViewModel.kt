package ru.kkalscan.presentation.scan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.repository.IScanRepository
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.util.kkalLog
import ru.kkalscan.domain.model.DishPortion
import ru.kkalscan.domain.model.MealType

class ScanViewModel(
    private val scanRepository: IScanRepository,
    private val diaryRepository: IDiaryRepository,
    private val scope: CoroutineScope,
) : IScanViewModel {

    private val _state = MutableStateFlow(ScanUiState())
    override val state: StateFlow<ScanUiState> = _state.asStateFlow()

    override suspend fun scanPhoto(photoBytes: ByteArray) {
        _state.update { it.copy(isLoading = true, errorMessage = null, limitHit = false) }
        kkalLog("Scan", "start photoBytes=${photoBytes.size}")
        runCatching { scanRepository.scanPhoto(photoBytes) }
            .onSuccess { result ->
                kkalLog(
                    "Scan",
                    "ok scanId=${result.scanId.take(8)}… dishes=${result.dishes.size} kcal=${result.totalKcal} left=${result.scansLeft}",
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                        baselineDishes = result.dishes,
                        photoBytes = photoBytes,
                        scansLeft = result.scansLeft,
                        selectedMealType = defaultMealType(),
                    )
                }
            }
            .onFailure { e ->
                kkalLog("Scan", "fail ${e::class.simpleName}: ${e.message}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.userMessage(),
                        limitHit = e is KkalScanException.LimitHit,
                        scansLeft = (e as? KkalScanException.LimitHit)?.scansLeft,
                    )
                }
            }
    }

    override suspend fun grantAdBonus() {
        runCatching { scanRepository.grantAdBonus() }
            .onSuccess { bonus ->
                _state.update { it.copy(scansLeft = bonus.scansLeft, limitHit = false, errorMessage = null) }
            }
            .onFailure { e ->
                _state.update { it.copy(errorMessage = e.userMessage()) }
            }
    }

    override fun selectMealType(mealType: MealType) {
        _state.update { it.copy(selectedMealType = mealType) }
    }

    override fun adjustDishGrams(index: Int, deltaGrams: Int) {
        updateDishAt(index) { dish -> DishPortion.withGrams(dish, dish.grams + deltaGrams) }
    }

    override fun scaleDishFromBaseline(index: Int, factor: Double) {
        val baseline = _state.value.baselineDishes.getOrNull(index) ?: return
        updateDishAt(index) { dish -> DishPortion.scaledFromBaseline(dish, baseline, factor) }
    }

    override fun removeDish(index: Int) {
        val current = _state.value.result?.dishes.orEmpty()
        if (index !in current.indices) return
        replaceDishes(current.filterIndexed { i, _ -> i != index })
    }

    override fun reset() {
        _state.value = ScanUiState(selectedMealType = _state.value.selectedMealType)
    }

    override fun onProActivated() {
        _state.update { it.copy(limitHit = false, errorMessage = null) }
    }

    override suspend fun addToDiary(): Result<Unit> {
        val result = _state.value.result ?: return Result.failure(IllegalStateException("No scan"))
        if (result.dishes.isEmpty()) {
            return Result.failure(IllegalStateException("No dishes"))
        }
        val scanId = result.scanId
        val mealType = _state.value.selectedMealType
        _state.update { it.copy(isSaving = true, saveSuccess = false, errorMessage = null) }
        kkalLog("Diary", "add scanId=${scanId.take(8)}… meal=$mealType dishes=${result.dishes.size}")
        return runCatching { diaryRepository.addFromScan(scanId, mealType, result.dishes) }
            .onSuccess { day ->
                kkalLog("Diary", "added entries=${day.entries.size} totalKcal=${day.totalKcal}")
                _state.update { it.copy(saveSuccess = true) }
            }
            .map { }
            .onFailure { e ->
                kkalLog("Diary", "add fail ${e.message}")
                _state.update { it.copy(isSaving = false, saveSuccess = false, errorMessage = e.userMessage()) }
            }
    }

    private fun updateDishAt(index: Int, transform: (ru.kkalscan.domain.model.Dish) -> ru.kkalscan.domain.model.Dish) {
        val current = _state.value.result?.dishes.orEmpty()
        if (index !in current.indices) return
        replaceDishes(current.mapIndexed { i, dish -> if (i == index) transform(dish) else dish })
    }

    private fun replaceDishes(dishes: List<ru.kkalscan.domain.model.Dish>) {
        val totals = DishPortion.totals(dishes)
        _state.update { state ->
            val result = state.result ?: return@update state
            state.copy(
                result = result.copy(
                    dishes = dishes,
                    totalKcal = totals.kcal,
                    totalProtein = totals.protein,
                    totalFat = totals.fat,
                    totalCarbs = totals.carbs,
                    totalFiber = totals.fiber,
                ),
            )
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.LimitHit -> "На сегодня бесплатные сканы закончились"
        is KkalScanException.Api -> message ?: "Не удалось распознать фото"
        else -> message ?: "Ошибка"
    }
}
