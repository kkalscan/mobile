package ru.kkalscan.data.storage

import java.util.UUID

internal actual fun deviceIdFromAndroidId(androidId: String): String =
    UUID.nameUUIDFromBytes("kkalscan:$androidId".toByteArray(Charsets.UTF_8)).toString()
