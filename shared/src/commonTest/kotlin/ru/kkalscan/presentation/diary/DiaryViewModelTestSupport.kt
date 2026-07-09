package ru.kkalscan.presentation.diary

import kotlinx.coroutines.CoroutineScope
import ru.kkalscan.TestApiFixtures
import ru.kkalscan.data.api.IKkalScanApi
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
    scope = scope,
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
