package ru.kkalscan

import ru.kkalscan.data.DefaultApiConfig
import ru.kkalscan.data.IApiConfig
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.api.KkalScanApi
import ru.kkalscan.data.createHttpClient
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.repository.IScanRepository
import ru.kkalscan.data.repository.ISubscriptionRepository
import ru.kkalscan.data.repository.ScanRepository
import ru.kkalscan.data.repository.SubscriptionRepository
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.presentation.diary.DiaryViewModel
import ru.kkalscan.presentation.diary.IDiaryViewModel
import ru.kkalscan.presentation.profile.IProfileViewModel
import ru.kkalscan.presentation.profile.ProfileViewModel
import ru.kkalscan.presentation.scan.IScanViewModel
import ru.kkalscan.presentation.scan.ScanViewModel

class AppDependencies(
    val apiConfig: IApiConfig = DefaultApiConfig,
    val deviceIdStorage: IDeviceIdStorage = InMemoryDeviceIdStorage(),
    val api: IKkalScanApi = KkalScanApi(createHttpClient(), apiConfig),
    val diaryRepository: IDiaryRepository = DiaryRepository(api, deviceIdStorage),
    val scanRepository: IScanRepository = ScanRepository(api, deviceIdStorage),
    val subscriptionRepository: ISubscriptionRepository = SubscriptionRepository(api, deviceIdStorage),
) {
    fun diaryViewModel(scope: kotlinx.coroutines.CoroutineScope): IDiaryViewModel =
        DiaryViewModel(diaryRepository, scope)

    fun scanViewModel(scope: kotlinx.coroutines.CoroutineScope): IScanViewModel =
        ScanViewModel(scanRepository, diaryRepository, scope)

    fun profileViewModel(scope: kotlinx.coroutines.CoroutineScope): IProfileViewModel =
        ProfileViewModel(subscriptionRepository, diaryRepository, scope)
}
