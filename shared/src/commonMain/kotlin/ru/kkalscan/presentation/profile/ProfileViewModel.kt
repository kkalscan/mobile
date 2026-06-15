package ru.kkalscan.presentation.profile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.repository.ISubscriptionRepository
import ru.kkalscan.domain.error.KkalScanException

class ProfileViewModel(
    private val subscriptionRepository: ISubscriptionRepository,
    private val diaryRepository: IDiaryRepository,
    private val scope: CoroutineScope,
) : IProfileViewModel {

    private val _state = MutableStateFlow(ProfileUiState(isLoading = true))
    override val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            val status = subscriptionRepository.getStatus()
            val day = diaryRepository.getToday()
            status to day.scansLeft
        }.onSuccess { (status, scansLeft) ->
            _state.update {
                it.copy(
                    isLoading = false,
                    status = status,
                    scansLeft = scansLeft,
                )
            }
        }.onFailure { e ->
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = when (e) {
                        is KkalScanException.Network -> "Нет сети. Проверьте подключение."
                        is KkalScanException.Api -> e.message ?: "Не удалось загрузить профиль"
                        else -> e.message ?: "Ошибка"
                    },
                )
            }
        }
    }
}
