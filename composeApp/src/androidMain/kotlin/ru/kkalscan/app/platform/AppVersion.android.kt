package ru.kkalscan.app.platform

import ru.kkalscan.app.BuildConfig

actual fun appVersionInfo(): AppVersionInfo =
    AppVersionInfo(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
    )
