package ru.kkalscan.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [DiaryDayEntity::class], version = 1, exportSchema = false)
@ConstructedBy(KkalScanDatabaseConstructor::class)
abstract class KkalScanDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
}

@Suppress("KotlinNoActualForExpect")
expect object KkalScanDatabaseConstructor : RoomDatabaseConstructor<KkalScanDatabase> {
    override fun initialize(): KkalScanDatabase
}
