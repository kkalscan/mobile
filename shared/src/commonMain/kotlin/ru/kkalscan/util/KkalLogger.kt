package ru.kkalscan.util

fun maskDeviceId(deviceId: String): String =
    if (deviceId.length > 8) "${deviceId.take(8)}…" else deviceId

expect fun kkalLog(tag: String, message: String)
