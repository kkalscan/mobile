package ru.kkalscan.data.storage

import kotlin.random.Random

internal actual fun randomDeviceId(): String {
    val bytes = ByteArray(16)
    Random.nextBytes(bytes)
    bytes[6] = (bytes[6].toInt() and 0x0f or 0x40).toByte()
    bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
    return bytes.toUuidString()
}

private fun ByteArray.toUuidString(): String {
    val hex = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
        "${hex.substring(16, 20)}-${hex.substring(20, 32)}"
}
