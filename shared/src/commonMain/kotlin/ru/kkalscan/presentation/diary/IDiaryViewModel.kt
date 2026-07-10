package ru.kkalscan.presentation.diary

import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.activity.CalorieBalance
import ru.kkalscan.domain.activity.StepSensorOnboardingPolicy
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.WorkoutParseResult

data class WorkoutParseUiState(
    val isLoading: Boolean = false,
    val preview: WorkoutParseResult? = null,
    val errorMessage: String? = null,
)

data class DiaryUiState(
    val isLoading: Boolean = false,
    val day: DiaryDay? = null,
    val balance: CalorieBalance? = null,
    val steps: Int? = null,
    val date: String? = null,
    val errorMessage: String? = null,
    val activitySource: ActivitySource = ActivitySource.None,
    val stepSensorAvailable: Boolean = false,
    val activityRecognitionGranted: Boolean = false,
    val workoutParse: WorkoutParseUiState = WorkoutParseUiState(),
) {
    val showActivityPermissionButton: Boolean
        get() = StepSensorOnboardingPolicy.shouldShowPermissionButton(this)
}

interface IDiaryViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<DiaryUiState>
    suspend fun refresh()
    suspend fun refreshActivityOnly()
    fun startActivityPolling()
    fun stopActivityPolling()
    suspend fun onForeground()
    suspend fun deleteEntry(entryId: String)
    suspend fun parseWorkoutDescription(description: String)
    suspend fun confirmParsedWorkout(): Boolean
    fun clearWorkoutParse()
    suspend fun addWorkout(name: String, kcal: Int)
    suspend fun deleteWorkout(workoutId: String)
    fun clearError()
    /** Effective today for journal when server activity lags behind live balance. */
    fun journalDayPatch(): DiaryDay?
}
