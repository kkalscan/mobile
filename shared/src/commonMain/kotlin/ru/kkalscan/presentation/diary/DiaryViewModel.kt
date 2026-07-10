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
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.repository.currentTimezoneOffsetMinutes
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.steps.ILocalStepCounter
import ru.kkalscan.data.steps.StepCounterStore
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.activity.ActivitySource
import ru.kkalscan.domain.activity.ActivitySourceResolver
import ru.kkalscan.domain.activity.CalorieBalanceCalculator
import ru.kkalscan.domain.activity.activitySourceFromWire
import ru.kkalscan.domain.activity.wireName
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.ActivityEmulator
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.util.kkalLog

class DiaryViewModel(
    private val diaryRepository: IDiaryRepository,
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
    private val stepCounterStore: StepCounterStore,
    private val localStepCounter: ILocalStepCounter,
    private val scope: CoroutineScope,
) : IDiaryViewModel {
    private val _state = MutableStateFlow(DiaryUiState(isLoading = true))
    override val state: StateFlow<DiaryUiState> = _state.asStateFlow()
    /** Bumped on every local diary mutation so in-flight refresh() cannot overwrite newer state. */
    private var dataGeneration = 0
    private var activityPollingJob: Job? = null
    private var cachedEmulator: ActivityEmulator? = null

    init { scope.launch { refresh() } }

    override suspend fun refresh() {
        val generation = ++dataGeneration
        val date = diaryRepository.currentDate()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            coroutineScope {
                val day = async { diaryRepository.getToday() }
                val activity = async { loadActivitySnapshot(fetchEmulator = true) }
                DiaryLoadResult(day.await(), activity.await())
            }
        }.onSuccess { r ->
            if (generation != dataGeneration) return@onSuccess
            val syncedDay = syncActivityIfNeeded(r.day, r.snapshot)
            applyActivitySnapshot(syncedDay, r.snapshot, isLoading = false, date = date)
            kkalLog(
                LOG_TAG,
                "refresh done date=$date eaten=${syncedDay.totalKcal} source=${r.snapshot.resolved.source} " +
                    "activity=${syncedDay.activityKcal} steps=${syncedDay.activitySteps}",
            )
        }.onFailure { e ->
            if (generation != dataGeneration) return@onFailure
            _state.update { it.copy(isLoading = false, date = date, errorMessage = e.userMessage()) }
            kkalLog(LOG_TAG, "refresh fail ${e::class.simpleName}: ${e.message}")
        }
    }

    override suspend fun refreshActivityOnly() {
        val day = _state.value.day ?: run {
            kkalLog(LOG_TAG, "activity poll skipped: diary not loaded yet")
            return
        }
        runCatching { loadActivitySnapshot(fetchEmulator = false) }
            .onSuccess { snapshot ->
                val prev = _state.value
                val syncedDay = syncActivityIfNeeded(day, snapshot)
                applyActivitySnapshot(syncedDay, snapshot, isLoading = prev.isLoading, date = prev.date ?: syncedDay.date)
                val balance = CalorieBalanceCalculator.compute(syncedDay, snapshot.resolved)
                val changed = prev.balance?.activityKcal != balance.activityKcal ||
                    prev.balance?.activitySource != balance.activitySource ||
                    prev.steps != snapshot.resolved.steps ||
                    prev.activityRecognitionGranted != snapshot.permissionGranted ||
                    prev.day?.totalBurnedKcal != syncedDay.totalBurnedKcal
                if (changed) {
                    kkalLog(
                        LOG_TAG,
                        "activity updated source=${snapshot.resolved.source} kcal=${balance.activityKcal} " +
                            "steps=${snapshot.resolved.steps} (was ${prev.steps})",
                    )
                }
            }
            .onFailure { e ->
                kkalLog(LOG_TAG, "activity poll fail ${e::class.simpleName}: ${e.message}")
            }
    }

    override fun startActivityPolling() {
        if (activityPollingJob?.isActive == true) return
        activityPollingJob = scope.launch {
            kkalLog(LOG_TAG, "activity polling started interval=${ACTIVITY_POLL_INTERVAL_MS}ms")
            refreshActivityOnly()
            while (isActive) {
                delay(ACTIVITY_POLL_INTERVAL_MS)
                refreshActivityOnly()
            }
        }
    }

    override fun stopActivityPolling() {
        if (activityPollingJob != null) {
            kkalLog(LOG_TAG, "activity polling stopped")
        }
        activityPollingJob?.cancel()
        activityPollingJob = null
    }

    override suspend fun onForeground() {
        val loaded = _state.value.date ?: return
        if (loaded != diaryRepository.currentDate()) {
            kkalLog(LOG_TAG, "onForeground date changed $loaded -> ${diaryRepository.currentDate()}, full refresh")
            refresh()
        } else {
            kkalLog(LOG_TAG, "onForeground same day, activity refresh only")
            refreshActivityOnly()
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        runCatching { diaryRepository.deleteEntry(entryId) }.onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } }
    }

    override suspend fun addWorkout(name: String, kcal: Int) {
        _state.update { it.copy(errorMessage = null) }
        runCatching { diaryRepository.addWorkout(name, kcal) }
            .onSuccess { day ->
                ++dataGeneration
                kkalLog(LOG_TAG, "workout saved kcal=$kcal, syncing activity")
                applyDayWithActivitySync(day)
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
        if (saved.isFailure) return false
        ++dataGeneration
        _state.update { it.copy(workoutParse = WorkoutParseUiState()) }
        val day = saved.getOrThrow()
        kkalLog(LOG_TAG, "workout confirmed ${preview.title} ${preview.burnedKcal} kcal, syncing activity")
        applyDayWithActivitySync(day)
        return true
    }

    override fun clearWorkoutParse() { _state.update { it.copy(workoutParse = WorkoutParseUiState()) } }

    override suspend fun deleteWorkout(workoutId: String) {
        runCatching { diaryRepository.deleteWorkout(workoutId) }.onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(errorMessage = e.userMessage()) } }
    }

    override fun clearError() { _state.update { it.copy(errorMessage = null) } }

    override fun journalDayPatch(): DiaryDay? {
        val s = _state.value
        val day = s.day ?: return null
        val balance = s.balance ?: return null
        val date = s.date ?: day.date
        if (date != day.date) return null
        return day.copy(
            totalBurnedKcal = balance.burnedKcal,
            activityKcal = balance.activityKcal,
            activitySteps = s.steps ?: day.activitySteps,
            activitySource = when (balance.activitySource) {
                ActivitySource.None -> day.activitySource
                else -> balance.activitySource.wireName()
            },
            netKcal = day.totalKcal - balance.burnedKcal,
        )
    }

    private suspend fun loadActivitySnapshot(fetchEmulator: Boolean): ActivitySnapshot = coroutineScope {
        val sensorAvailable = async { localStepCounter.isSensorAvailable() }
        val permissionGranted = async { localStepCounter.hasPermission() }
        val sensorSteps = async { stepCounterStore.readTodaySteps() }
        val emulator = if (fetchEmulator) {
            async {
                runCatching {
                    api.getActivityEmulator(deviceIdStorage.getDeviceId(), currentTimezoneOffsetMinutes())
                }.onSuccess { cachedEmulator = it }.getOrNull()
            }
        } else {
            null
        }
        val resolved = ActivitySourceResolver.resolve(
            sensorSteps = sensorSteps.await(),
            sensorAvailable = sensorAvailable.await(),
            sensorPermissionGranted = permissionGranted.await(),
            emulator = emulator?.await() ?: cachedEmulator,
        )
        ActivitySnapshot(
            resolved = resolved,
            sensorAvailable = sensorAvailable.await(),
            permissionGranted = permissionGranted.await(),
        )
    }

    private fun applyActivitySnapshot(
        day: DiaryDay,
        snapshot: ActivitySnapshot,
        isLoading: Boolean,
        date: String,
    ) {
        _state.update {
            it.copy(
                isLoading = isLoading,
                day = day,
                balance = CalorieBalanceCalculator.compute(day, snapshot.resolved),
                steps = day.activitySteps ?: snapshot.resolved.steps,
                date = date,
                activitySource = if (day.activitySteps != null || day.activityKcal > 0 || !day.activitySource.isNullOrBlank()) {
                    activitySourceFromWire(day.activitySource)
                } else {
                    snapshot.resolved.source
                },
                stepSensorAvailable = snapshot.sensorAvailable,
                activityRecognitionGranted = snapshot.permissionGranted,
            )
        }
    }

    private suspend fun applyDayWithActivitySync(day: DiaryDay) {
        val date = diaryRepository.currentDate()
        runCatching {
            val snapshot = loadActivitySnapshot(fetchEmulator = false)
            val syncedDay = syncActivityIfNeeded(day, snapshot)
            applyActivitySnapshot(syncedDay, snapshot, isLoading = false, date = date)
            kkalLog(
                LOG_TAG,
                "day synced burned=${syncedDay.totalBurnedKcal} activity=${syncedDay.activityKcal} " +
                    "workouts=${syncedDay.workouts.sumOf { it.kcal }}",
            )
        }.onFailure { e ->
            kkalLog(LOG_TAG, "applyDayWithActivitySync fail ${e::class.simpleName}: ${e.message}")
            _state.update { it.copy(errorMessage = e.userMessage()) }
        }
    }

    private suspend fun syncActivityIfNeeded(day: DiaryDay, snapshot: ActivitySnapshot): DiaryDay {
        val local = snapshot.resolved
        if (local.source != ActivitySource.DeviceSensor) return day
        if (local.activeKcal <= 0) return day
        val localSteps = local.steps ?: 0
        val savedSteps = day.activitySteps ?: 0
        if (local.activeKcal == day.activityKcal && localSteps == savedSteps) return day
        return runCatching {
            diaryRepository.syncActivity(localSteps, local.activeKcal, local.source)
        }.onFailure { e ->
            kkalLog(LOG_TAG, "activity sync fail ${e::class.simpleName}: ${e.message}")
        }.getOrDefault(day)
    }

    private fun Throwable.userMessage(isWorkoutParse: Boolean = false) = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.LimitHit -> message ?: "Лимит сканов исчерпан"
        is KkalScanException.Api -> message ?: if (isWorkoutParse) "Не удалось понять описание тренировки" else "Ошибка сервера"
        else -> message ?: "Неизвестная ошибка"
    }

    private data class DiaryLoadResult(val day: DiaryDay, val snapshot: ActivitySnapshot)

    private data class ActivitySnapshot(
        val resolved: ru.kkalscan.domain.activity.ResolvedActivity,
        val sensorAvailable: Boolean,
        val permissionGranted: Boolean,
    )

    private companion object {
        const val LOG_TAG = "Diary"
        const val ACTIVITY_POLL_INTERVAL_MS = 60_000L
    }
}
