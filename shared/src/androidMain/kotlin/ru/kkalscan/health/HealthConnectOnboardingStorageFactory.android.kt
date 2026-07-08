package ru.kkalscan.health

import ru.kkalscan.data.storage.AndroidDeviceIdContext

private const val PREFS_NAME = "kkalscan"
private const val KEY_INITIAL_PROMPT_SHOWN = "health_connect_initial_prompt_shown"

actual fun createHealthConnectOnboardingStorage(): HealthConnectOnboardingStorage =
    AndroidHealthConnectOnboardingStorage()

private class AndroidHealthConnectOnboardingStorage : HealthConnectOnboardingStorage {
    private val prefs by lazy {
        AndroidDeviceIdContext.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }

    override fun wasInitialPromptShown(): Boolean =
        prefs.getBoolean(KEY_INITIAL_PROMPT_SHOWN, false)

    override fun markInitialPromptShown() {
        prefs.edit().putBoolean(KEY_INITIAL_PROMPT_SHOWN, true).apply()
    }
}
