package ru.kkalscan.presentation.scan

import ru.kkalscan.domain.model.MealType
import ru.kkalscan.domain.model.ScanResult
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ScanUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
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
    val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return when (hour) {
        in 5..10 -> MealType.breakfast
        in 11..15 -> MealType.lunch
        in 16..21 -> MealType.dinner
        else -> MealType.snack
    }
}
