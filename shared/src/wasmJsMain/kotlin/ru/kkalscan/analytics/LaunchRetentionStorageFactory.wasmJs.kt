package ru.kkalscan.analytics

import kotlinx.browser.localStorage

actual fun createLaunchRetentionStorage(): LaunchRetentionStorage =
    PersistentLaunchRetentionStorage(
        readFirstLaunch = { localStorage.getItem(KEY_FIRST_LAUNCH) },
        writeFirstLaunch = { localStorage.setItem(KEY_FIRST_LAUNCH, it) },
        readDay1 = { localStorage.getItem(KEY_DAY1)?.toBooleanStrictOrNull() ?: false },
        writeDay1 = { localStorage.setItem(KEY_DAY1, it.toString()) },
        readDay7 = { localStorage.getItem(KEY_DAY7)?.toBooleanStrictOrNull() ?: false },
        writeDay7 = { localStorage.setItem(KEY_DAY7, it.toString()) },
    )

private const val KEY_FIRST_LAUNCH = "kkalscan_first_launch_date"
private const val KEY_DAY1 = "kkalscan_day_1_return_sent"
private const val KEY_DAY7 = "kkalscan_day_7_return_sent"
