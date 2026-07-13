package ru.kkalscan.data.local

import kotlinx.coroutines.flow.Flow
import ru.kkalscan.domain.model.DiaryDay

/**
 * Local cache for diary days. Room-backed on Android/JVM; in-memory on wasm/tests.
 */
interface IDiaryLocalStore {
    fun observeDay(date: String): Flow<DiaryDay?>
    suspend fun getDay(date: String): DiaryDay?
    suspend fun getDays(dates: List<String>): Map<String, DiaryDay>
    suspend fun upsert(day: DiaryDay)
}
