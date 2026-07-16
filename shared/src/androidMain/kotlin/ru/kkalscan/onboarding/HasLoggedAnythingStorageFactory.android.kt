package ru.kkalscan.onboarding

actual fun createHasLoggedAnythingStorage(): HasLoggedAnythingStorage =
    AndroidHasLoggedAnythingStorage()

private class AndroidHasLoggedAnythingStorage : HasLoggedAnythingStorage {
    private val prefs by lazy {
        ru.kkalscan.data.storage.AndroidDeviceIdContext.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }

    override fun hasLoggedAnything(): Boolean =
        prefs.getBoolean(KEY_HAS_LOGGED_ANYTHING, false)

    override fun markLoggedAnything() {
        prefs.edit().putBoolean(KEY_HAS_LOGGED_ANYTHING, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "kkalscan"
        const val KEY_HAS_LOGGED_ANYTHING = "has_logged_anything"
    }
}
