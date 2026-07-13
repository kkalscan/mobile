package ru.kkalscan.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import ru.kkalscan.domain.model.SubscriptionStatus

class RoomProfileLocalStore(
    private val dao: ProfileDao,
    private val json: Json = DiaryLocalJson.json,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : IProfileLocalStore {
    override fun observeSubscription(): Flow<SubscriptionStatus?> =
        dao.observeSubscription().map { entity -> entity?.let { decode(it.payloadJson) } }

    override suspend fun getSubscription(): SubscriptionStatus? =
        dao.getSubscription()?.let { decode(it.payloadJson) }

    override suspend fun upsert(status: SubscriptionStatus) {
        dao.upsert(
            ProfileSubscriptionEntity(
                payloadJson = json.encodeToString(SubscriptionStatus.serializer(), status),
                updatedAt = clock(),
            ),
        )
    }

    private fun decode(payload: String): SubscriptionStatus =
        json.decodeFromString(SubscriptionStatus.serializer(), payload)
}

fun createRoomProfileLocalStore(database: KkalScanDatabase): IProfileLocalStore =
    RoomProfileLocalStore(database.profileDao())
