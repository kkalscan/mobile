package ru.kkalscan.domain.activity

enum class ActivitySource {
    DeviceSensor,
    Emulator,
    None,
}

fun ActivitySource.wireName(): String =
    when (this) {
        ActivitySource.DeviceSensor -> "device_sensor"
        ActivitySource.Emulator -> "emulator"
        ActivitySource.None -> "none"
    }

fun activitySourceFromWire(raw: String?): ActivitySource =
    when (raw?.trim()?.lowercase()) {
        "device_sensor" -> ActivitySource.DeviceSensor
        "emulator" -> ActivitySource.Emulator
        else -> ActivitySource.None
    }
