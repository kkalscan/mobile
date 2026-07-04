package ru.kkalscan.analytics

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LaunchRetentionTrackerTest {

    @Test
    fun firstLaunch_doesNotSendRetentionEvents() {
        val storage = InMemoryLaunchRetentionStorage()
        val events = mutableListOf<String>()
        val tracker = LaunchRetentionTracker(storage, events::add)

        tracker.onAppLaunch(LocalDate(2026, 7, 4))

        assertEquals(LocalDate(2026, 7, 4), storage.firstLaunch)
        assertTrue(events.isEmpty())
    }

    @Test
    fun day1Return_firesOnce() {
        val storage = InMemoryLaunchRetentionStorage(firstLaunch = LocalDate(2026, 7, 3))
        val events = mutableListOf<String>()
        val tracker = LaunchRetentionTracker(storage, events::add)

        tracker.onAppLaunch(LocalDate(2026, 7, 4))
        tracker.onAppLaunch(LocalDate(2026, 7, 5))

        assertEquals(listOf(AnalyticsEvents.DAY_1_RETURN), events)
    }

    @Test
    fun day7Return_firesOnce() {
        val storage = InMemoryLaunchRetentionStorage(firstLaunch = LocalDate(2026, 6, 27))
        val events = mutableListOf<String>()
        val tracker = LaunchRetentionTracker(storage, events::add)

        tracker.onAppLaunch(LocalDate(2026, 7, 4))

        assertEquals(
            listOf(AnalyticsEvents.DAY_1_RETURN, AnalyticsEvents.DAY_7_RETURN),
            events,
        )
    }

    private class InMemoryLaunchRetentionStorage(
        var firstLaunch: LocalDate? = null,
        var day1Sent: Boolean = false,
        var day7Sent: Boolean = false,
    ) : LaunchRetentionStorage {
        override fun readFirstLaunchDate(): LocalDate? = firstLaunch
        override fun writeFirstLaunchDate(date: LocalDate) {
            firstLaunch = date
        }

        override fun readDay1Sent(): Boolean = day1Sent
        override fun writeDay1Sent(sent: Boolean) {
            day1Sent = sent
        }

        override fun readDay7Sent(): Boolean = day7Sent
        override fun writeDay7Sent(sent: Boolean) {
            day7Sent = sent
        }
    }
}
