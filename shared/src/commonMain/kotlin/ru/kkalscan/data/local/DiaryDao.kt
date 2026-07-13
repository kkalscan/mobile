package ru.kkalscan.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_days WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DiaryDayEntity?>

    @Query("SELECT * FROM diary_days WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DiaryDayEntity?

    @Query("SELECT * FROM diary_days WHERE date IN (:dates)")
    suspend fun getByDates(dates: List<String>): List<DiaryDayEntity>

    @Upsert
    suspend fun upsert(entity: DiaryDayEntity)
}
