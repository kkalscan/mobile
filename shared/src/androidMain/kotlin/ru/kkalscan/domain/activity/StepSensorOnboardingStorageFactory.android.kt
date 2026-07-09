package ru.kkalscan.domain.activity

actual fun createStepSensorOnboardingStorage(): StepSensorOnboardingStorage =
    AndroidStepSensorOnboardingStorage()

private class AndroidStepSensorOnboardingStorage : StepSensorOnboardingStorage {
    private val prefs by lazy {
        ru.kkalscan.data.storage.AndroidDeviceIdContext.appContext
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }

    override fun wasInitialPromptShown(): Boolean =
        prefs.getBoolean(KEY_INITIAL_PROMPT_SHOWN, false)

    override fun markInitialPromptShown() {
        prefs.edit().putBoolean(KEY_INITIAL_PROMPT_SHOWN, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "kkalscan"
        const val KEY_INITIAL_PROMPT_SHOWN = "step_sensor_initial_prompt_shown"
    }
}
