package ru.kkalscan.presentation.diary

import kotlinx.coroutines.CoroutineScope
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.profile.IEnergyProfileStorage
import ru.kkalscan.data.profile.InMemoryEnergyProfileStorage
import ru.kkalscan.data.steps.IStepBaselineStorage
import ru.kkalscan.data.steps.ILocalStepCounter
import ru.kkalscan.data.steps.InMemoryStepBaselineStorage
import ru.kkalscan.data.steps.StepCounterStore
import ru.kkalscan.data.repository.IDiaryRepository
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage

fun createDiaryViewModelForTest(
    diaryRepository: IDiaryRepository,
    scope: CoroutineScope,
    api: IKkalScanApi = TestApiFixtures.api(),
    deviceIdStorage: IDeviceIdStorage = InMemoryDeviceIdStorage().apply {
        setDeviceId(TestApiFixtures.DEVICE_ID)
    },
    localStepCounter: ILocalStepCounter = FakeLocalStepCounter(),
    stepBaselineStorage: IStepBaselineStorage = InMemoryStepBaselineStorage(),
    energyProfileStorage: IEnergyProfileStorage = InMemoryEnergyProfileStorage(),
    refreshOnInit: Boolean = true,
): DiaryViewModel = DiaryViewModel(
    diaryRepository = diaryRepository,
    api = api,
    deviceIdStorage = deviceIdStorage,
    stepCounterStore = StepCounterStore(
        localStepCounter,
        stepBaselineStorage,
        todayProvider = { diaryRepository.currentDate() },
    ),
    localStepCounter = localStepCounter,
    energyProfileStorage = energyProfileStorage,
    scope = scope,
    refreshOnInit = refreshOnInit,
)

/** Prefer [kotlinx.coroutines.test.TestScope.backgroundScope] so diary Flow collectors do not fail runTest. */
fun kotlinx.coroutines.test.TestScope.createDiaryViewModel(
    diaryRepository: IDiaryRepository,
    api: IKkalScanApi = TestApiFixtures.api(),
    deviceIdStorage: IDeviceIdStorage = InMemoryDeviceIdStorage().apply {
        setDeviceId(TestApiFixtures.DEVICE_ID)
    },
    localStepCounter: ILocalStepCounter = FakeLocalStepCounter(),
    stepBaselineStorage: IStepBaselineStorage = InMemoryStepBaselineStorage(),
    energyProfileStorage: IEnergyProfileStorage = InMemoryEnergyProfileStorage(),
    refreshOnInit: Boolean = false,
): DiaryViewModel = createDiaryViewModelForTest(
    diaryRepository = diaryRepository,
    scope = backgroundScope,
    api = api,
    deviceIdStorage = deviceIdStorage,
    localStepCounter = localStepCounter,
    stepBaselineStorage = stepBaselineStorage,
    energyProfileStorage = energyProfileStorage,
    refreshOnInit = refreshOnInit,
)

/** Offline Flow tests: controlled refresh on the test scheduler with explicit teardown. */
fun kotlinx.coroutines.test.TestScope.createOfflineDiaryViewModel(
    diaryRepository: IDiaryRepository,
    api: IKkalScanApi = TestApiFixtures.api(),
): DiaryViewModel = createDiaryViewModelForTest(
    diaryRepository = diaryRepository,
    scope = this,
    api = api,
    refreshOnInit = false,
)

class FakeLocalStepCounter(
    private val sensorAvailable: Boolean = false,
    private val permissionGranted: Boolean = false,
    var cumulativeSteps: Long? = null,
) : ILocalStepCounter {
    var readCalls = 0
        private set

    override suspend fun isSensorAvailable(): Boolean = sensorAvailable
    override suspend fun hasPermission(): Boolean = permissionGranted
    override suspend fun readCumulativeSteps(): Long? {
        readCalls++
        return cumulativeSteps
    }
}
