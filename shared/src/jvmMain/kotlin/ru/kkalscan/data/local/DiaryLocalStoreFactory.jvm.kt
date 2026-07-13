package ru.kkalscan.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun createDiaryLocalStore(): IDiaryLocalStore {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "kkalscan.db")
    val database = Room.databaseBuilder<KkalScanDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    return createRoomDiaryLocalStore(database)
}
