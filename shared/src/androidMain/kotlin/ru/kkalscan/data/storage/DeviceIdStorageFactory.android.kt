package ru.kkalscan.data.storage

import android.content.Context

private const val PREFS_NAME = "kkalscan"
private const val PREFS_DEVICE_ID = "device_id"

object AndroidDeviceIdContext {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

actual fun createDeviceIdStorage(): IDeviceIdStorage {
    val prefs = AndroidDeviceIdContext.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return PersistentDeviceIdStorage(
        readStored = { prefs.getString(PREFS_DEVICE_ID, null) },
        writeStored = { prefs.edit().putString(PREFS_DEVICE_ID, it).apply() },
    )
}
