package ru.kkalscan.presentation.diary

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.kkalscan.data.health.IHealthConnectReader
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.domain.activity.CalorieBalanceCalculator
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.util.kkalLog

class DiaryViewModel(
    private val diaryRepository: IDiaryRepository,
    private val healthConnect: IHealthConnectReader,
    private val scope: CoroutineScope,
) : IDiaryViewModel {
    private val _state = MutableStateFlow(DiaryUiState(isLoading = true))
    override val state: StateFlow<DiaryUiState> = _state.asStateFlow()
    /** Bumped on every local diary mutation so in-flight refresh() cannot overwrite newer state. */
    private var dataGeneration = 0
    private var healthConnectPollingJob: Job? = null

    init { scope.launch { refresh() } }

    override suspend fun refresh() {
        val generation = ++dataGeneration
        val date = diaryRepository.currentDate()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            coroutineScope {
                val day = async { diaryRepository.getToday() }
                val hc = async { readHealthConnectSnapshot() }
                val d = day.await()
                val snapshot = hc.await()
                DiaryLoadResult(d, snapshot)
            }
        }.onSuccess { r ->
            if (generation != dataGeneration) return@onSuccess
            applyHealthConnectSnapshot(r.day, r.snapshot, isLoading = false, date = date)
            kkalLog(
                LOG_TAG,
                "refresh done date=$date eaten=${r.day.totalKcal} hcKcal=${r.snapshot.activeCalories} steps=${r.snapshot.steps} " +
                    "hcAvail=${r.snapshot.available} hcPerm=${r.snapshot.permissionsGranted}",
            )
        }.onFailure { e ->
            if (generation != dataGeneration) return@onFailure
            _state.update { it.copy(isLoading = false, date = date, errorMessage = e.userMessage()) }
            kkalLog(LOG_TAG, "refresh fail ${e::class.simpleName}: ${e.message}")
        }
    }

    override suspend fun refreshHealthConnectOnly() {
        val day = _state.value.day ?: run {
            kkalLog(LOG_TAG, "health connect poll skipped: diary not loaded yet")
            return
        }
        runCatching { readHealthConnectSnapshot() }
            .onSuccess { snapshot ->
                val prev = _state.value
                applyHealthConnectSnapshot(day, snapshot, isLoading = prev.isLoading, date = prev.date ?: day.date)
                val changed = prev.balance?.healthConnectKcal != snapshot.activeCalories ||
                    prev.steps != snapshot.steps ||
                    prev.healthConnectPermissionsGranted != snapshot.permissionsGranted
                if (changed) {
                    kkalLog(
                        LOG_TAG,
                        "health connect updated hcKcal=${snapshot.activeCalories} (was ${prev.balance?.healthConnectKcal}) " +
                            "steps=${snapshot.steps} (was ${prev.steps}) perm=${snapshot.permissionsGranted}",
                    )
                }
            }
            .onFailure { e ->
                kkalLog(LOG_TAG, "health connect poll fail ${e::class.simpleName}: ${e.message}")
            }
    }

    override fun startHealthConnectPolling() {
        if (healthConnectPollingJob?.isActive == true) return
        healthConnectPollingJob = scope.launch {
            kkalLog(LOG_TAG, "health connect polling started interval=${HEALTH_CONNECT_POLL_INTERVAL_MS}ms")
            refreshHealthConnectOnly()
            while (isActive) {
                delay(HEALTH_CONNECT_POLL_INTERVAL_MS)
                refreshHealthConnectOnly()
            }
        }
    }

    override fun stopHealthConnectPolling() {
        if (healthConnectPollingJob != null) {
            kkalLog(LOG_TAG, "health connect polling stopped")
        }
        healthConnectPollingJob?.cancel()
        healthConnectPollingJob = null
    }

    override suspend fun onForeground() {
        val loaded = _state.value.date ?: return
        if (loaded != diaryRepository.currentDate()) {
            kkalLog(LOG_TAG, "onForeground date changed $loaded -> ${diaryRepository.currentDate()}, full refresh")
            refresh()
        } else {
            kkalLog(LOG_TAG, "onForeground same day, health connect refresh only")
            refreshHealthConnectOnly()
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        runCatching { diaryRepository.deleteEntry(entryId) }.onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } }
    }

    override suspend fun addWorkout(name: String, kcal: Int) {
        _state.update { it.copy(errorMessage = null) }
        runCatching { diaryRepository.addWorkout(name, kcal) }
            .onSuccess {
                kkalLog(LOG_TAG, "workout saved kcal=$kcal, refreshing day")
                refresh()
            }
            .onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } }
    }

    override suspend fun parseWorkoutDescription(description: String) {
        _state.update { it.copy(workoutParse = WorkoutParseUiState(isLoading = true), errorMessage = null) }
        runCatching { diaryRepository.parseWorkout(description) }
            .onSuccess { preview -> _state.update { it.copy(workoutParse = WorkoutParseUiState(isLoading = false, preview = preview)) } }
            .onFailure { e -> _state.update { it.copy(workoutParse = WorkoutParseUiState(isLoading = false, errorMessage = e.userMessage(true))) } }
    }

    override suspend fun confirmParsedWorkout(): Boolean {
        val preview = _state.value.workoutParse.preview ?: return false
        _state.update { it.copy(workoutParse = it.workoutParse.copy(isLoading = true, errorMessage = null)) }
        val saved = runCatching { diaryRepository.addWorkout(preview.title, preview.burnedKcal) }
            .onFailure { e -> _state.update { it.copy(workoutParse = it.workoutParse.copy(isLoading = false, errorMessage = e.userMessage())) } }
            .isSuccess
        if (!saved) return false
        ++dataGeneration
        _state.update { it.copy(workoutParse = WorkoutParseUiState()) }
        kkalLog(LOG_TAG, "workout confirmed ${preview.title} ${preview.burnedKcal} kcal, refreshing day")
        refresh()
        return true
    }

    override fun clearWorkoutParse() { _state.update { it.copy(workoutParse = WorkoutParseUiState()) } }

    override suspend fun deleteWorkout(workoutId: String) {
        runCatching { diaryRepository.deleteWorkout(workoutId) }.onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } }
    }

    override fun clearError() { _state.update { it.copy(errorMessage = null) } }

    private suspend fun readHealthConnectSnapshot(): HealthConnectSnapshot {
        return coroutineScope {
            val avail = async { healthConnect.isAvailable() }
            val perm = async { healthConnect.hasPermissions() }
            val hcKcal = async { healthConnect.readTodayActiveCalories() }
            val hcSteps = async { healthConnect.readTodaySteps() }
            HealthConnectSnapshot(
                available = avail.await(),
                permissionsGranted = perm.await(),
                activeCalories = hcKcal.await(),
                steps = hcSteps.await(),
            )
        }
    }

    private fun applyHealthConnectSnapshot(
        day: DiaryDay,
        snapshot: HealthConnectSnapshot,
        isLoading: Boolean,
        date: String,
    ) {
        _state.update {
            it.copy(
                isLoading = isLoading,
                day = day,
                balance = CalorieBalanceCalculator.compute(day, snapshot.activeCalories),
                steps = snapshot.steps,
                date = date,
                healthConnectAvailable = snapshot.available,
                healthConnectPermissionsGranted = snapshot.permissionsGranted,
            )
        }
    }

    private fun Throwable.userMessage(isWorkoutParse: Boolean = false) = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.LimitHit -> message ?: "Лимит сканов исчерпан"
        is KkalScanException.Api -> message ?: if (isWorkoutParse) "Не удалось понять описание тренировки" else "Ошибка сервера"
        else -> message ?: "Неизвестная ошибка"
    }

    private data class DiaryLoadResult(val day: DiaryDay, val snapshot: HealthConnectSnapshot)

    private data class HealthConnectSnapshot(
        val available: Boolean,
        val permissionsGranted: Boolean,
        val activeCalories: Int,
        val steps: Int?,
    )

    private companion object {
        const val LOG_TAG = "Diary"
        const val HEALTH_CONNECT_POLL_INTERVAL_MS = 60_000L
    }
}
