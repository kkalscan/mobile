package ru.kkalscan.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import ru.kkalscan.domain.model.DiaryDay

internal object DiaryLocalJson {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}

class RoomDiaryLocalStore(
    private val dao: DiaryDao,
    private val json: Json = DiaryLocalJson.json,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : IDiaryLocalStore {
    override fun observeDay(date: String): Flow<DiaryDay?> =
        dao.observeByDate(date).map { entity -> entity?.let { decode(it.payloadJson) } }

    override suspend fun getDay(date: String): DiaryDay? =
        dao.getByDate(date)?.let { decode(it.payloadJson) }

    override suspend fun getDays(dates: List<String>): Map<String, DiaryDay> =
        dao.getByDates(dates).associate { it.date to decode(it.payloadJson) }

    override suspend fun upsert(day: DiaryDay) {
        dao.upsert(
            DiaryDayEntity(
                date = day.date,
                payloadJson = json.encodeToString(DiaryDay.serializer(), day),
                updatedAt = clock(),
            ),
        )
    }

    private fun decode(payload: String): DiaryDay =
        json.decodeFromString(DiaryDay.serializer(), payload)
}

fun createRoomDiaryLocalStore(database: KkalScanDatabase): IDiaryLocalStore =
    RoomDiaryLocalStore(database.diaryDao())
