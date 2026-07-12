package ru.kkalscan.data.storage

import ru.kkalscan.util.kkalLog
import ru.kkalscan.util.maskDeviceId

private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

class PersistentDeviceIdStorage(
    private val readStored: () -> String?,
    private val writeStored: (String) -> Unit,
    private val generateId: () -> String = { randomDeviceId() },
) : IDeviceIdStorage {

    private var cached: String? = null

    override fun getDeviceId(): String {
        cached?.let { return it }
        readStored()?.takeIf { it.matches(uuidRegex) }?.let {
            cached = it
            kkalLog("DeviceId", "restored ${maskDeviceId(it)}")
            return it
        }
        return generateId().also { id ->
            cached = id
            writeStored(id)
            kkalLog("DeviceId", "created ${maskDeviceId(id)}")
        }
    }

    override fun setDeviceId(id: String) {
        cached = id
        writeStored(id)
        kkalLog("DeviceId", "set ${maskDeviceId(id)}")
    }
}
