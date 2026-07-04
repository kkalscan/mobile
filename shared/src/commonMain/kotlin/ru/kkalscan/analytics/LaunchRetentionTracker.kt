package ru.kkalscan.analytics

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

interface LaunchRetentionStorage {
    fun readFirstLaunchDate(): LocalDate?
    fun writeFirstLaunchDate(date: LocalDate)
    fun readDay1Sent(): Boolean
    fun writeDay1Sent(sent: Boolean)
    fun readDay7Sent(): Boolean
    fun writeDay7Sent(sent: Boolean)
}

/**
 * Fires [AnalyticsEvents.DAY_1_RETURN] and [AnalyticsEvents.DAY_7_RETURN] once per install.
 */
class LaunchRetentionTracker(
    private val storage: LaunchRetentionStorage,
    private val report: (String) -> Unit,
) {
    fun onAppLaunch(now: LocalDate) {
        val firstLaunch = storage.readFirstLaunchDate() ?: run {
            storage.writeFirstLaunchDate(now)
            return
        }
        val daysSinceInstall = daysBetween(firstLaunch, now)
        if (daysSinceInstall >= 1 && !storage.readDay1Sent()) {
            report(AnalyticsEvents.DAY_1_RETURN)
            storage.writeDay1Sent(true)
        }
        if (daysSinceInstall >= 7 && !storage.readDay7Sent()) {
            report(AnalyticsEvents.DAY_7_RETURN)
            storage.writeDay7Sent(true)
        }
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Int {
        var days = 0
        var cursor = from
        while (cursor < to) {
            cursor = cursor.plus(DatePeriod(days = 1))
            days++
        }
        return days
    }
}
