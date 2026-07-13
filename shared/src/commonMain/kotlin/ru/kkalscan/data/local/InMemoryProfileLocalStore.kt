package ru.kkalscan.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import ru.kkalscan.domain.model.SubscriptionStatus

class InMemoryProfileLocalStore(
    private val json: Json = DiaryLocalJson.json,
) : IProfileLocalStore {
    private val mutex = Mutex()
    private val payload = MutableStateFlow<String?>(null)

    override fun observeSubscription(): Flow<SubscriptionStatus?> =
        payload.map { it?.let { json.decodeFromString(SubscriptionStatus.serializer(), it) } }

    override suspend fun getSubscription(): SubscriptionStatus? = mutex.withLock {
        payload.value?.let { json.decodeFromString(SubscriptionStatus.serializer(), it) }
    }

    override suspend fun upsert(status: SubscriptionStatus) {
        mutex.withLock {
            payload.value = json.encodeToString(SubscriptionStatus.serializer(), status)
        }
    }
}
