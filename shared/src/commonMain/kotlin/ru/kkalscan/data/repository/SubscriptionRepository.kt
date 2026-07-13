package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.local.IProfileLocalStore
import ru.kkalscan.data.local.InMemoryProfileLocalStore
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.ProSubscriptionStart
import ru.kkalscan.domain.model.SubscriptionStatus

interface ISubscriptionRepository {
    suspend fun getStatus(): SubscriptionStatus
    suspend fun startPro(): ProSubscriptionStart
}

class SubscriptionRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
    private val localStore: IProfileLocalStore = InMemoryProfileLocalStore(),
) : ISubscriptionRepository {

    override suspend fun getStatus(): SubscriptionStatus {
        runCatching { refreshFromNetwork() }
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

    private suspend fun refreshFromNetwork() {
        val deviceId = deviceIdStorage.getDeviceId()
        localStore.upsert(api.getSubscriptionStatus(deviceId))
    }
}
