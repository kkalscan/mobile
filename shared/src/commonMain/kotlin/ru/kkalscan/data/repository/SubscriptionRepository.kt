package ru.kkalscan.data.repository

import ru.kkalscan.data.api.IKkalScanApi
import ru.kkalscan.data.storage.IDeviceIdStorage
import ru.kkalscan.domain.model.SubscriptionStatus

interface ISubscriptionRepository {
    suspend fun getStatus(): SubscriptionStatus
}

class SubscriptionRepository(
    private val api: IKkalScanApi,
    private val deviceIdStorage: IDeviceIdStorage,
) : ISubscriptionRepository {

    override suspend fun getStatus(): SubscriptionStatus {
        val deviceId = deviceIdStorage.getDeviceId()
        return api.getSubscriptionStatus(deviceId)
    }
}
