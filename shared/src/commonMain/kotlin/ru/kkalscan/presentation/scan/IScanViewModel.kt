package ru.kkalscan.presentation.scan

import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ScanResult

data class ScanUiState(
    val isLoading: Boolean = false,
    val result: ScanResult? = null,
    val selectedMealType: MealType = defaultMealType(),
    val errorMessage: String? = null,
    val limitHit: Boolean = false,
    val scansLeft: Int? = null,
)

interface IScanViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<ScanUiState>
    suspend fun scanPhoto(photoBytes: ByteArray)
    suspend fun grantAdBonus()
    suspend fun addToDiary(): Result<Unit>
    fun selectMealType(mealType: MealType)
    fun reset()
}

fun defaultMealType(): MealType {
    val hour = kotlinx.datetime.Clock.System.now()
        .toEpochMilliseconds()
        .let { ms ->
            // fallback lunch; platform tests override via selectMealType
            ((ms / 3_600_000) % 24).toInt()
        }
    return when (hour) {
        in 5..10 -> MealType.breakfast
        in 11..15 -> MealType.lunch
        in 16..21 -> MealType.dinner
        else -> MealType.snack
    }
}
