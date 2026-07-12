package ru.kkalscan.data.storage

/**
 * Deterministic guest device_id from Android [Settings.Secure.ANDROID_ID].
 * Same hardware id → same UUID across reinstalls (until factory reset / user change).
 */
internal expect fun deviceIdFromAndroidId(androidId: String): String
