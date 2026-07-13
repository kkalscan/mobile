package ru.kkalscan

import ru.kkalscan.data.api.FakeKkalScanApi
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.local.IDiaryLocalStore
import ru.kkalscan.data.local.IProfileLocalStore
import ru.kkalscan.data.local.InMemoryDiaryLocalStore
import ru.kkalscan.data.local.InMemoryProfileLocalStore
import ru.kkalscan.data.repository.ProfileRepository
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.error.KkalScanException
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.SubscriptionStatus

fun offlineProfileRepo(
    api: IKkalScanApi,
    profileStore: IProfileLocalStore = InMemoryProfileLocalStore(),
    diaryStore: IDiaryLocalStore = InMemoryDiaryLocalStore(),
    today: String = TestApiFixtures.TODAY,
    storage: IDeviceIdStorage = offlineTestDeviceIdStorage(),
): ProfileRepository = ProfileRepository(
    api = api,
    deviceIdStorage = storage,
    localStore = profileStore,
    diaryRepository = offlineDiaryRepo(api, diaryStore, today, storage),
)

fun sampleCachedSubscription(
    isPro: Boolean = false,
    accountLinked: Boolean = false,
): SubscriptionStatus = SubscriptionStatus(
    isPro = isPro,
    accountLinked = accountLinked,
)

fun sampleCachedDayWithScans(
    scansLeft: Int = 3,
    date: String = TestApiFixtures.TODAY,
): DiaryDay = sampleCachedDay(date = date).copy(scansLeft = scansLeft)

class FailingProfileApi : IKkalScanApi by FakeKkalScanApi() {
    var getSubscriptionStatusCalls = 0
        private set

    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus {
        getSubscriptionStatusCalls++
        throw KkalScanException.Network("offline")
    }
}

class RecordingProfileApi(
    private val networkStatus: SubscriptionStatus,
) : IKkalScanApi by FakeKkalScanApi() {
    var getSubscriptionStatusCalls = 0
        private set

    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus {
        getSubscriptionStatusCalls++
        return networkStatus
    }
}

class RecordingProfileFailingDiaryApi(
    private val networkStatus: SubscriptionStatus,
) : IKkalScanApi by FakeKkalScanApi() {
    var getSubscriptionStatusCalls = 0
        private set

    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus {
        getSubscriptionStatusCalls++
        return networkStatus
    }

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay =
        throw KkalScanException.Network("offline")
}

class FailingProfileAndDiaryApi : IKkalScanApi by FakeKkalScanApi() {
    override suspend fun getSubscriptionStatus(deviceId: String): SubscriptionStatus =
        throw KkalScanException.Network("offline")

    override suspend fun getDiary(deviceId: String, date: String, timezoneOffsetMinutes: Int): DiaryDay =
        throw KkalScanException.Network("offline")
}
