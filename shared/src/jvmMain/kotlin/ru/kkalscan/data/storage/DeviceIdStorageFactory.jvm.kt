package ru.kkalscan.data.storage

actual fun createDeviceIdStorage(): IDeviceIdStorage = InMemoryDeviceIdStorage()
