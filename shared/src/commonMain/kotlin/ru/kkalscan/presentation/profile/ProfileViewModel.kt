package ru.kkalscan.presentation.profile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kkalscan.data.profile.IEnergyProfileStorage
import ru.kkalscan.data.repository.IBugReportRepository
import ru.kkalscan.data.repository.IProfileRepository
import ru.kkalscan.domain.activity.BmrCalculator
import ru.kkalscan.domain.activity.EnergyProfile
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.ProSubscriptionStart

class ProfileViewModel(
    private val profileRepository: IProfileRepository,
    private val bugReportRepository: IBugReportRepository,
    private val energyProfileStorage: IEnergyProfileStorage,
    private val scope: CoroutineScope,
    private val refreshOnInit: Boolean = true,
) : IProfileViewModel {

    private val _state = MutableStateFlow(ProfileUiState(isLoading = true))
    override val state: StateFlow<ProfileUiState> = _state.asStateFlow()
    private var profileObserveJob: Job? = null
    private var refreshJob: Job? = null

    init {
        if (refreshOnInit) scope.launch { refresh() }
    }

    override suspend fun refresh() {
        refreshJob?.cancelAndJoin()
        coroutineScope {
            refreshJob = coroutineContext[Job]
            refreshBody()
        }
    }

    private suspend fun refreshBody() {
        profileObserveJob?.cancel()
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val firstEmission = kotlinx.coroutines.CompletableDeferred<Unit>()
        profileObserveJob = scope.launch {
            var signaled = false
            profileRepository.observeProfile().collect { resource ->
                val profile = energyProfileStorage.getProfile() ?: EnergyProfile()
                if (resource.status != null) {
                    _state.update {
                        it.copy(
                            isLoading = resource.isRefreshing && it.status == null,
                            status = resource.status,
                            scansLeft = resource.scansLeft,
                            energyProfile = profile,
                            errorMessage = resource.error?.userMessage(),
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = resource.isRefreshing,
                            scansLeft = resource.scansLeft,
                            energyProfile = profile,
                            errorMessage = resource.error?.userMessage(),
                        )
                    }
                }
                if (!signaled && (resource.status != null || !resource.isRefreshing)) {
                    signaled = true
                    firstEmission.complete(Unit)
                }
            }
        }
        firstEmission.await()
    }

    override suspend fun startProSubscription(): ProSubscriptionStart =
        profileRepository.startPro()

    override suspend fun submitBugReport(
        email: String,
        description: String,
        screenshots: List<ByteArray>,
    ) {
        _state.update {
            it.copy(
                bugReportSubmitting = true,
                bugReportError = null,
                bugReportSuccess = null,
            )
        }
        runCatching {
            bugReportRepository.submit(email, description, screenshots)
        }.onSuccess { result ->
            _state.update {
                it.copy(
                    bugReportSubmitting = false,
                    bugReportSuccess = result,
                )
            }
            refresh()
        }.onFailure { e ->
            _state.update {
                it.copy(
                    bugReportSubmitting = false,
                    bugReportError = e.userMessage(),
                )
            }
        }
    }

    override fun saveEnergyProfile(profile: EnergyProfile): Boolean {
        val normalized = profile.normalized()
        energyProfileStorage.saveProfile(normalized)
        _state.update {
            it.copy(
                energyProfile = normalized,
                profileSaved = true,
                clearEnergyFieldFocus = true,
            )
        }
        return true
    }

    override fun clearProfileSaved() {
        _state.update { it.copy(profileSaved = false) }
    }

    override fun consumeClearEnergyFieldFocus() {
        _state.update { it.copy(clearEnergyFieldFocus = false) }
    }

    override fun clearBugReportFeedback() {
        _state.update { it.copy(bugReportSuccess = null, bugReportError = null) }
    }

    /** Stops profile Flow collectors; used from unit tests to avoid leaking jobs in [runTest]. */
    internal fun tearDownForTest() {
        refreshJob?.cancel()
        refreshJob = null
        profileObserveJob?.cancel()
        profileObserveJob = null
    }

    private fun Throwable.userMessage() = when (this) {
        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
        is KkalScanException.Api -> message ?: "Не удалось загрузить профиль"
        else -> message ?: "Ошибка"
    }
}

fun ProfileUiState.dailyBmrKcal(): Int = BmrCalculator.dailyBmr(energyProfile)
