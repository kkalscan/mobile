package ru.kkalscan.analytics

actual fun createLaunchRetentionStorage(): LaunchRetentionStorage =
    PersistentLaunchRetentionStorage(
        readFirstLaunch = { readPref(KEY_FIRST_LAUNCH) },
        writeFirstLaunch = { writePref(KEY_FIRST_LAUNCH, it) },
        readDay1 = { readPref(KEY_DAY1)?.toBooleanStrictOrNull() ?: false },
        writeDay1 = { writePref(KEY_DAY1, it.toString()) },
        readDay7 = { readPref(KEY_DAY7)?.toBooleanStrictOrNull() ?: false },
        writeDay7 = { writePref(KEY_DAY7, it.toString()) },
    )

private const val PREFS_NAME = "kkalscan"
private const val KEY_FIRST_LAUNCH = "first_launch_date"
private const val KEY_DAY1 = "day_1_return_sent"
private const val KEY_DAY7 = "day_7_return_sent"

private fun readPref(key: String): String? {
    val prefs = ru.kkalscan.data.storage.AndroidDeviceIdContext.appContext
        .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    return prefs.getString(key, null)
}

private fun writePref(key: String, value: String) {
    val prefs = ru.kkalscan.data.storage.AndroidDeviceIdContext.appContext
        .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    prefs.edit().putString(key, value).apply()
}
