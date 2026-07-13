package ru.kkalscan

import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.local.IDiaryLocalStore
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.data.repository.DiaryRepository
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.data.storage.InMemoryDeviceIdStorage
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.DiaryEntry
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType

fun offlineTestDeviceIdStorage(): IDeviceIdStorage =
    InMemoryDeviceIdStorage().apply { setDeviceId(TestApiFixtures.DEVICE_ID) }

fun offlineDiaryRepo(
    api: IKkalScanApi,
    store: IDiaryLocalStore = InMemoryDiaryLocalStore(),
    today: String = TestApiFixtures.TODAY,
    storage: IDeviceIdStorage = offlineTestDeviceIdStorage(),
): DiaryRepository = DiaryRepository(
    api = api,
    deviceIdStorage = storage,
    localStore = store,
    todayProvider = { today },
)

fun sampleCachedDay(
    date: String = TestApiFixtures.TODAY,
    totalKcal: Int = 120,
): DiaryDay = DiaryDay(
    date = date,
    totalKcal = totalKcal,
    entries = listOf(
        DiaryEntry(
            id = "cached-entry",
            createdAt = "${date}T08:00:00Z",
            mealType = MealType.breakfast,
            totalKcal = totalKcal,
            dishes = listOf(Dish("Каша", 200, totalKcal, 8.0, 4.0, 30.0)),
        ),
    ),
)

class FailingDiaryApi : IKkalScanApi by FakeKkalScanApi() {
    var getDiaryCalls = 0
        private set

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay {
        getDiaryCalls++
        throw KkalScanException.Network("offline")
    }
}

class RecordingDiaryApi(
    private val networkDay: DiaryDay,
) : IKkalScanApi by FakeKkalScanApi() {
    var getDiaryCalls = 0
        private set

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay {
        getDiaryCalls++
        return networkDay
    }
}
