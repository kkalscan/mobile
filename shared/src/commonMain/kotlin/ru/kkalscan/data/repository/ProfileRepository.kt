package ru.kkalscan.data.repository

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.local.IProfileLocalStore
import ru.kkalscan.data.local.InMemoryProfileLocalStore
import ru.kkalscan.data.local.ProfileResource
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.ProSubscriptionStart
import ru.kkalscan.domain.model.SubscriptionStatus
import ru.kkalscan.util.kkalLog
import ru.kkalscan.util.maskDeviceId

interface IProfileRepository {
    fun observeProfile(timezoneOffsetMinutes: Int = currentTimezoneOffsetMinutes()): Flow<ProfileResource>
    suspend fun getStatus(): SubscriptionStatus
    suspend fun startPro(): ProSubscriptionStart
}

class ProfileRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
    private val localStore: IProfileLocalStore = InMemoryProfileLocalStore(),
    private val diaryRepository: IDiaryRepository,
) : IProfileRepository {

    override fun observeProfile(timezoneOffsetMinutes: Int): Flow<ProfileResource> = flow {
        val subscriptionRefreshing = MutableStateFlow(true)
        val subscriptionError = MutableStateFlow<Throwable?>(null)
        coroutineScope {
            launch {
                runCatching { refreshSubscriptionFromNetwork() }
                    .onSuccess {
                        subscriptionError.value = null
                        kkalLog("Profile", "observeProfile subscription refresh ok")
                    }
                    .onFailure { e ->
                        subscriptionError.value = e
                        kkalLog("Profile", "observeProfile subscription refresh fail ${e::class.simpleName}: ${e.message}")
                    }
                subscriptionRefreshing.value = false
            }
            combine(
                localStore.observeSubscription(),
                diaryRepository.observeToday(timezoneOffsetMinutes),
                subscriptionRefreshing,
                subscriptionError,
            ) { status, diaryResource, isSubscriptionRefreshing, error ->
                val isRefreshing = isSubscriptionRefreshing || diaryResource.isRefreshing
                val hasCache = status != null || diaryResource.day != null
                ProfileResource(
                    status = status,
                    scansLeft = diaryResource.day?.scansLeft,
                    isRefreshing = isRefreshing,
                    error = when {
                        !isRefreshing && !hasCache -> error ?: diaryResource.error
                        !isSubscriptionRefreshing && status == null -> error
                        else -> null
                    },
                )
            }.collect { emit(it) }
        }
    }

    override suspend fun getStatus(): SubscriptionStatus {
        runCatching { refreshSubscriptionFromNetwork() }
            .onFailure { e ->
                localStore.getSubscription()?.let { return it }
                throw e
            }
        return localStore.getSubscription()
            ?: error("Subscription status missing after refresh")
    }

    override suspend fun startPro(): ProSubscriptionStart {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.startProSubscription(deviceId)
    }

    private suspend fun refreshSubscriptionFromNetwork() {
        val deviceId = deviceIdStorage.getDeviceId()
        val status = api.getSubscriptionStatus(deviceId)
        localStore.upsert(status)
        kkalLog("Profile", "getStatus device=${maskDeviceId(deviceId)} isPro=${status.isPro}")
    }
}
