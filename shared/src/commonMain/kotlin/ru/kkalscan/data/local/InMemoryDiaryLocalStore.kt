package ru.kkalscan.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import ru.kkalscan.domain.model.DiaryDay

class InMemoryDiaryLocalStore(
    private val json: Json = DiaryLocalJson.json,
) : IDiaryLocalStore {
    private val mutex = Mutex()
    private val days = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observeDay(date: String): Flow<DiaryDay?> =
        days.map { map -> map[date]?.let { decode(it) } }

    override suspend fun getDay(date: String): DiaryDay? = mutex.withLock {
        days.value[date]?.let { decode(it) }
    }

    override suspend fun getDays(dates: List<String>): Map<String, DiaryDay> = mutex.withLock {
        dates.mapNotNull { date ->
            days.value[date]?.let { date to decode(it) }
        }.toMap()
    }

    override suspend fun upsert(day: DiaryDay) {
        mutex.withLock {
            days.update { it + (day.date to json.encodeToString(DiaryDay.serializer(), day)) }
        }
    }

    private fun decode(payload: String): DiaryDay =
        json.decodeFromString(DiaryDay.serializer(), payload)
}
