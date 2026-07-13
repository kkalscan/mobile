package ru.kkalscan.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import ru.kkalscan.data.storage.AndroidDeviceIdContext

actual fun createDiaryLocalStore(): IDiaryLocalStore {
    val context = AndroidDeviceIdContext.appContext
    val dbFile = context.getDatabasePath("kkalscan.db")
    val database = Room.databaseBuilder<KkalScanDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    return createRoomDiaryLocalStore(database)
}
