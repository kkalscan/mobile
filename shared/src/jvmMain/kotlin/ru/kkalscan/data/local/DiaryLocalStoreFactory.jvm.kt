package ru.kkalscan.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

private val database: KkalScanDatabase by lazy {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "kkalscan.db")
    Room.databaseBuilder<KkalScanDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2)
        .build()
}

actual fun createDiaryLocalStore(): IDiaryLocalStore =
    createRoomDiaryLocalStore(database)

actual fun createProfileLocalStore(): IProfileLocalStore =
    createRoomProfileLocalStore(database)
