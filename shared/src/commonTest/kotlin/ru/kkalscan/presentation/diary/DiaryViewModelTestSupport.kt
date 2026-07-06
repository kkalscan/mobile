package ru.kkalscan.presentation.diary

import kotlinx.coroutines.CoroutineScope
import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.health.IHealthConnectReader
import ru.kkalscan.data.health.NoOpHealthConnectReader
import ru.kkalscan.data.repository.ActivityRepository
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.data.storage.InMemoryWorkoutStorage
import ru.kkalscan.data.storage.IWorkoutStorage

fun createDiaryViewModelForTest(
    diaryRepository: IDiaryRepository,
    scope: CoroutineScope,
    deviceIdStorage: IDeviceIdStorage = InMemoryDeviceIdStorage().apply {
        setDeviceId("11111111-1111-1111-1111-111111111111")
    },
    api: IKkalScanApi = FakeKkalScanApi(),
    healthConnect: IHealthConnectReader = NoOpHealthConnectReader(),
    workoutStorage: IWorkoutStorage = InMemoryWorkoutStorage(),
    todayProvider: () -> String = { "2026-07-04" },
): DiaryViewModel {
    val activityRepository = ActivityRepository(
        api = api,
        deviceIdStorage = deviceIdStorage,
        healthConnect = healthConnect,
        workoutStorage = workoutStorage,
        todayProvider = todayProvider,
    )
    return DiaryViewModel(
        diaryRepository = diaryRepository,
        activityRepository = activityRepository,
        healthConnect = healthConnect,
        scope = scope,
    )
}
