package ru.kkalscan.presentation.profile

import ru.kkalscan.domain.activity.EnergyProfile
import ru.kkalscan.domain.model.BugReportResult
import ru.kkalscan.domain.model.ProSubscriptionStart
import ru.kkalscan.domain.model.SubscriptionStatus

data class ProfileUiState(
    val isLoading: Boolean = false,
    val status: SubscriptionStatus? = null,
    val scansLeft: Int? = null,
    val energyProfile: EnergyProfile = EnergyProfile(),
    val profileSaved: Boolean = false,
    /** UI should clear focus from weight/height/age fields, then call [IProfileViewModel.consumeClearEnergyFieldFocus]. */
    val clearEnergyFieldFocus: Boolean = false,
    val errorMessage: String? = null,
    val bugReportSubmitting: Boolean = false,
    val bugReportSuccess: BugReportResult? = null,
    val bugReportError: String? = null,
)

interface IProfileViewModel {
    val state: kotlinx.coroutines.flow.StateFlow<ProfileUiState>
    suspend fun refresh()
    suspend fun startProSubscription(): ProSubscriptionStart
    suspend fun submitBugReport(email: String, description: String, screenshots: List<ByteArray>)
    fun saveEnergyProfile(profile: EnergyProfile): Boolean
    fun clearProfileSaved()
    fun consumeClearEnergyFieldFocus()
    fun clearBugReportFeedback()
}
