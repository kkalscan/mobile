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
        runCatching { scanRepository.scanPhoto(photoBytes) }
            .onSuccess { result ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                        scansLeft = result.scansLeft,
                    )
                }
            }
            .onFailure { e ->
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

    override fun reset() {
        _state.value = ScanUiState(selectedMealType = _state.value.selectedMealType)
    }

    override suspend fun addToDiary(): Result<Unit> {
        val scanId = _state.value.result?.scanId ?: return Result.failure(IllegalStateException("No scan"))
        val mealType = _state.value.selectedMealType
        return runCatching { diaryRepository.addFromScan(scanId, mealType) }.map { }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.LimitHit -> "На сегодня бесплатные сканы закончились"
        is KkalScanException.Api -> message ?: "Не удалось распознать фото"
        else -> message ?: "Ошибка"
    }
}
