package ru.kkalscan

import ru.kkalscan.data.DefaultApiConfig
import ru.kkalscan.data.IApiConfig
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.api.KkalScanApi
import ru.kkalscan.data.createHttpClient
import ru.kkalscan.data.repository.BugReportRepository
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.repository.FeatureSearchRepository
import ru.kkalscan.data.repository.FoodSearchRepository
import ru.kkalscan.data.repository.IBugReportRepository
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.repository.IFeatureSearchRepository
import ru.kkalscan.data.repository.IFoodSearchRepository
import ru.kkalscan.data.repository.IInsightRepository
import ru.kkalscan.data.repository.InsightRepository
import ru.kkalscan.data.repository.IScanRepository
import ru.kkalscan.data.repository.ISubscriptionRepository
import ru.kkalscan.data.repository.ScanRepository
import ru.kkalscan.data.repository.SubscriptionRepository
import ru.kkalscan.data.health.IHealthConnectReader
import ru.kkalscan.data.health.createHealthConnectReader
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.data.storage.createDeviceIdStorage
import ru.kkalscan.presentation.features.FeatureSearchViewModel
import ru.kkalscan.presentation.features.IFeatureSearchViewModel
import ru.kkalscan.presentation.food.FoodSearchViewModel
import ru.kkalscan.presentation.food.IFoodSearchViewModel
import ru.kkalscan.presentation.diary.DiaryViewModel
import ru.kkalscan.presentation.diary.IDiaryViewModel
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
    val diaryRepository: IDiaryRepository = DiaryRepository(api, deviceIdStorage),
    val scanRepository: IScanRepository = ScanRepository(api, deviceIdStorage),
    val subscriptionRepository: ISubscriptionRepository = SubscriptionRepository(api, deviceIdStorage),
    val insightRepository: IInsightRepository = InsightRepository(deviceIdStorage),
    val foodSearchRepository: IFoodSearchRepository = FoodSearchRepository(api, deviceIdStorage),
    val featureSearchRepository: IFeatureSearchRepository = FeatureSearchRepository(api, deviceIdStorage),
    val bugReportRepository: IBugReportRepository = BugReportRepository(api, deviceIdStorage),
    val healthConnectReader: IHealthConnectReader = createHealthConnectReader(),
) {
    fun diaryViewModel(scope: kotlinx.coroutines.CoroutineScope): IDiaryViewModel =
        DiaryViewModel(diaryRepository, healthConnectReader, scope)

    fun foodSearchViewModel(scope: kotlinx.coroutines.CoroutineScope): IFoodSearchViewModel =
        FoodSearchViewModel(foodSearchRepository, diaryRepository, scope)

    fun featureSearchViewModel(
        scope: kotlinx.coroutines.CoroutineScope,
        onSearchCompleted: ru.kkalscan.presentation.features.FeatureSearchCompletedListener = { _, _ -> },
    ): IFeatureSearchViewModel =
        FeatureSearchViewModel(featureSearchRepository, scope, onSearchCompleted)

    fun journalViewModel(scope: kotlinx.coroutines.CoroutineScope): IJournalViewModel =
        JournalViewModel(diaryRepository, insightRepository, scope)

    fun scanViewModel(scope: kotlinx.coroutines.CoroutineScope): IScanViewModel =
        ScanViewModel(scanRepository, diaryRepository, scope)

    fun profileViewModel(scope: kotlinx.coroutines.CoroutineScope): IProfileViewModel =
        ProfileViewModel(subscriptionRepository, diaryRepository, bugReportRepository, scope)
}
