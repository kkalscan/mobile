package ru.kkalscan.data.steps

import android.content.Context
import ru.kkalscan.data.storage.AndroidDeviceIdContext

actual fun createStepBaselineStorage(): IStepBaselineStorage =
    AndroidStepBaselineStorage(AndroidDeviceIdContext.appContext)

private class AndroidStepBaselineStorage(context: Context) : IStepBaselineStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getBaseline(dateIso: String): Long? {
        val value = prefs.getLong(key(dateIso), -1L)
        return value.takeIf { it >= 0L }
    }

    override fun setBaseline(dateIso: String, cumulativeSteps: Long) {
        prefs.edit().putLong(key(dateIso), cumulativeSteps).apply()
    }

    private fun key(dateIso: String) = "baseline_$dateIso"

    private companion object {
        const val PREFS_NAME = "kkalscan_step_baseline"
    }
}
