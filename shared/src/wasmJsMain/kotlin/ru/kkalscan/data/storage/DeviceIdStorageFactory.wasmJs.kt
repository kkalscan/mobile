package ru.kkalscan.data.storage

import kotlinx.browser.localStorage

private const val STORAGE_KEY = "kkalscan_device_id"

actual fun createDeviceIdStorage(): IDeviceIdStorage = PersistentDeviceIdStorage(
    readStored = { localStorage.getItem(STORAGE_KEY) },
    writeStored = { localStorage.setItem(STORAGE_KEY, it) },
)
