package ru.kkalscan.domain.activity

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class ActivityEmulatorTimeProrationTest {

    @Test
    fun beforeDaylightReturnsZero() {
        val now = instant(2026, 7, 9, 6, 30, offsetMinutes = 180)
        assertEquals(0, ActivityEmulatorTimeProration.prorateForDaylight(1500, 180, now))
    }

    @Test
    fun midDaylightReturnsLinearFraction() {
        val now = instant(2026, 7, 9, 15, 0, offsetMinutes = 180)
        assertEquals(750, ActivityEmulatorTimeProration.prorateForDaylight(1500, 180, now))
    }

    @Test
    fun afterDaylightReturnsFullAmount() {
        val now = instant(2026, 7, 9, 23, 0, offsetMinutes = 180)
        assertEquals(1500, ActivityEmulatorTimeProration.prorateForDaylight(1500, 180, now))
    }

    private fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int, offsetMinutes: Int): Instant {
        val zone = TimeZone.of(offsetId(offsetMinutes))
        return LocalDateTime(year, month, day, hour, minute).toInstant(zone)
    }

    private fun offsetId(offsetMinutes: Int): String {
        val sign = if (offsetMinutes >= 0) "+" else "-"
        val abs = kotlin.math.abs(offsetMinutes)
        val hours = abs / 60
        val minutes = abs % 60
        return "GMT$sign${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }
}
