package ru.kkalscan

import ru.kkalscan.data.DefaultApiConfig
import ru.kkalscan.data.IApiConfig
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.api.KkalScanApi
import ru.kkalscan.data.createHttpClient
import ru.kkalscan.data.local.IDiaryLocalStore
import ru.kkalscan.data.local.createDiaryLocalStore
import ru.kkalscan.data.local.createProfileLocalStore
import ru.kkalscan.data.repository.BugReportRepository
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.repository.FeatureSearchRepository
import ru.kkalscan.data.repository.IBugReportRepository
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.repository.IFeatureSearchRepository
import ru.kkalscan.data.repository.IInsightRepository
import ru.kkalscan.data.repository.InsightRepository
import ru.kkalscan.data.repository.IProfileRepository
import ru.kkalscan.data.repository.IScanRepository
import ru.kkalscan.data.repository.ISubscriptionRepository
import ru.kkalscan.data.repository.ProfileRepository
import ru.kkalscan.data.repository.ScanRepository
import ru.kkalscan.data.repository.SubscriptionRepository
import ru.kkalscan.data.profile.createEnergyProfileStorage
import ru.kkalscan.data.profile.IEnergyProfileStorage
import ru.kkalscan.data.steps.createLocalStepCounter
import ru.kkalscan.data.steps.createStepBaselineStorage
import ru.kkalscan.data.steps.StepCounterStore
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.data.storage.createDeviceIdStorage
import ru.kkalscan.onboarding.FirstLogTracker
import ru.kkalscan.onboarding.HasLoggedAnythingStorage
import ru.kkalscan.onboarding.createHasLoggedAnythingStorage
import ru.kkalscan.presentation.diary.DiaryViewModel
import ru.kkalscan.presentation.diary.IDiaryViewModel
import ru.kkalscan.presentation.features.FeatureSearchViewModel
import ru.kkalscan.presentation.features.IFeatureSearchViewModel
import ru.kkalscan.presentation.journal.IJournalViewModel
import ru.kkalscan.presentation.journal.JournalViewModel
import ru.kkalscan.presentation.profile.IProfileViewModel
import ru.kkalscan.presentation.profile.ProfileViewModel
import ru.kkalscan.presentation.scan.IScanViewModel
import ru.kkalscan.presentation.scan.ScanViewModel

class AppDependencies(
    val apiConfig: IApiConfig = DefaultApiConfig,
    val deviceIdStorage: IDeviceIdStorage = createDeviceIdStorage(),
    val api: IKkalScanApi = KkalScanApi(createHttpClient(), apiConfig),
    val diaryLocalStore: IDiaryLocalStore = createDiaryLocalStore(),
    val profileLocalStore: ru.kkalscan.data.local.IProfileLocalStore = createProfileLocalStore(),
    val diaryRepository: IDiaryRepository = DiaryRepository(api, deviceIdStorage, diaryLocalStore),
    val scanRepository: IScanRepository = ScanRepository(api, deviceIdStorage),
    val profileRepository: IProfileRepository = ProfileRepository(api, deviceIdStorage, profileLocalStore, diaryRepository),
    val subscriptionRepository: ISubscriptionRepository = SubscriptionRepository(api, deviceIdStorage, profileLocalStore),
    val insightRepository: IInsightRepository = InsightRepository(deviceIdStorage),
    val featureSearchRepository: IFeatureSearchRepository = FeatureSearchRepository(api, deviceIdStorage),
    val bugReportRepository: IBugReportRepository = BugReportRepository(api, deviceIdStorage),
    val energyProfileStorage: IEnergyProfileStorage = createEnergyProfileStorage(),
    val hasLoggedAnythingStorage: HasLoggedAnythingStorage = createHasLoggedAnythingStorage(),
    private val localStepCounter: ru.kkalscan.data.steps.ILocalStepCounter = createLocalStepCounter(),
    private val stepCounterStore: StepCounterStore = StepCounterStore(
        localStepCounter,
        createStepBaselineStorage(),
        todayProvider = { diaryRepository.currentDate() },
    ),
) {
    private val firstLogTracker = FirstLogTracker(hasLoggedAnythingStorage)

    fun diaryViewModel(scope: kotlinx.coroutines.CoroutineScope): IDiaryViewModel =
        DiaryViewModel(
            diaryRepository = diaryRepository,
            api = api,
            deviceIdStorage = deviceIdStorage,
            stepCounterStore = stepCounterStore,
            localStepCounter = localStepCounter,
            energyProfileStorage = energyProfileStorage,
            scope = scope,
            firstLogTracker = firstLogTracker,
        )

    fun featureSearchViewModel(
        scope: kotlinx.coroutines.CoroutineScope,
        onSearchCompleted: ru.kkalscan.presentation.features.FeatureSearchCompletedListener = { _, _ -> },
        onFoodIntentAnalytics: ru.kkalscan.presentation.features.FeatureSearchFoodIntentAnalytics = { _, _ -> },
    ): IFeatureSearchViewModel =
        FeatureSearchViewModel(
            featureSearchRepository,
            scope,
            onSearchCompleted,
            onFoodIntentAnalytics,
        )

    fun journalViewModel(
        scope: kotlinx.coroutines.CoroutineScope,
        todayPatchProvider: () -> ru.kkalscan.domain.model.DiaryDay? = { null },
    ): IJournalViewModel =
        JournalViewModel(diaryRepository, insightRepository, scope, todayPatchProvider)

    fun scanViewModel(scope: kotlinx.coroutines.CoroutineScope): IScanViewModel =
        ScanViewModel(scanRepository, diaryRepository, scope, firstLogTracker)

    fun profileViewModel(scope: kotlinx.coroutines.CoroutineScope): IProfileViewModel =
        ProfileViewModel(profileRepository, bugReportRepository, energyProfileStorage, scope)
}
