package ru.kkalscan.data.storage

class InMemoryDeviceIdStorage : IDeviceIdStorage {
    private var deviceId: String? = null

    override fun getDeviceId(): String =
        deviceId ?: randomDeviceId().also { deviceId = it }

    override fun setDeviceId(id: String) {
        deviceId = id
    }
}

internal expect fun randomDeviceId(): String
