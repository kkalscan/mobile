package ru.kkalscan.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import ru.kkalscan.data.storage.AndroidDeviceIdContext

private val database: KkalScanDatabase by lazy {
    val context = AndroidDeviceIdContext.appContext
    val dbFile = context.getDatabasePath("kkalscan.db")
    Room.databaseBuilder<KkalScanDatabase>(
        context = context,
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
