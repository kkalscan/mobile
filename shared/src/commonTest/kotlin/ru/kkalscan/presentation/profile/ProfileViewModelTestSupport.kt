package ru.kkalscan.presentation.profile

import kotlinx.coroutines.CoroutineScope
import ru.kkalscan.data.profile.IEnergyProfileStorage
import ru.kkalscan.data.profile.InMemoryEnergyProfileStorage
import ru.kkalscan.data.repository.IBugReportRepository
import ru.kkalscan.data.repository.IProfileRepository
import ru.kkalscan.domain.model.BugReportResult

fun createProfileViewModelForTest(
    profileRepository: IProfileRepository,
    scope: CoroutineScope,
    bugReportRepository: IBugReportRepository = FakeBugReportRepository(),
    energyProfileStorage: IEnergyProfileStorage = InMemoryEnergyProfileStorage(),
    refreshOnInit: Boolean = false,
): ProfileViewModel = ProfileViewModel(
    profileRepository = profileRepository,
    bugReportRepository = bugReportRepository,
    energyProfileStorage = energyProfileStorage,
    scope = scope,
    refreshOnInit = refreshOnInit,
)

fun kotlinx.coroutines.test.TestScope.createOfflineProfileViewModel(
    profileRepository: IProfileRepository,
    energyProfileStorage: IEnergyProfileStorage = InMemoryEnergyProfileStorage(),
): ProfileViewModel = createProfileViewModelForTest(
    profileRepository = profileRepository,
    scope = this,
    energyProfileStorage = energyProfileStorage,
    refreshOnInit = false,
)

private class FakeBugReportRepository : IBugReportRepository {
    override suspend fun submit(
        email: String,
        description: String,
        screenshots: List<ByteArray>,
    ): BugReportResult = BugReportResult(
        reportId = "bug-1",
        isPro = false,
        message = "ok",
    )
}
