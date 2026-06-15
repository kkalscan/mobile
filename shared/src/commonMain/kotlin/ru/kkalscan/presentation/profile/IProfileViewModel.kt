package ru.kkalscan.presentation.profile

import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.SubscriptionStatus

data class ProfileUiState(
    val isLoading: Boolean = false,
    val status: SubscriptionStatus? = null,
    val scansLeft: Int? = null,
    val errorMessage: String? = null,
    val bugReportSubmitting: Boolean = false,
    val bugReportSuccess: BugReportResult? = null,
    val bugReportError: String? = null,
)

interface IProfileViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<ProfileUiState>
    suspend fun refresh()
    suspend fun submitBugReport(email: String, description: String, screenshots: List<ByteArray>)
    fun clearBugReportFeedback()
}
