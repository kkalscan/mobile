package ru.kkalscan.app.platform

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Int,
)

/** App version taken from the platform build config (Android) or a placeholder (web). */
expect fun appVersionInfo(): AppVersionInfo
