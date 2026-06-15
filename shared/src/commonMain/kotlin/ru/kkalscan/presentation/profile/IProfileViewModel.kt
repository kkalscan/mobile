package ru.kkalscan.presentation.profile

import ru.kkalscan.domain.model.SubscriptionStatus

data class ProfileUiState(
    val isLoading: Boolean = false,
    val status: SubscriptionStatus? = null,
    val scansLeft: Int? = null,
    val errorMessage: String? = null,
)

interface IProfileViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<ProfileUiState>
    suspend fun refresh()
}
