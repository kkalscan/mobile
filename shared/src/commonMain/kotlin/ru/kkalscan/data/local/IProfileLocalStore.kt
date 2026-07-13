package ru.kkalscan.data.local

import kotlinx.coroutines.flow.Flow
import ru.kkalscan.domain.model.SubscriptionStatus

interface IProfileLocalStore {
    fun observeSubscription(): Flow<SubscriptionStatus?>
    suspend fun getSubscription(): SubscriptionStatus?
    suspend fun upsert(status: SubscriptionStatus)
}
