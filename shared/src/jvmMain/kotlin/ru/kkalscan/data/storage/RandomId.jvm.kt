package ru.kkalscan.data.storage

import java.util.UUID

internal actual fun randomDeviceId(): String = UUID.randomUUID().toString()
