package ru.kkalscan.domain.activity

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

object ActivityEmulatorTimeProration {
    const val FULL_DAYLIGHT_ACTIVE_KCAL = 1500
    const val DAYLIGHT_START_HOUR = 7
    const val DAYLIGHT_END_HOUR = 23

    fun prorateForDaylight(
        fullDayKcal: Int,
        timezoneOffsetMinutes: Int,
        now: Instant = Clock.System.now(),
    ): Int {
        if (fullDayKcal <= 0) return 0
        val zone = TimeZone.of(offsetId(timezoneOffsetMinutes))
        val local = now.toLocalDateTime(zone)
        val startMinutes = DAYLIGHT_START_HOUR * 60
        val endMinutes = DAYLIGHT_END_HOUR * 60
        val totalMinutes = endMinutes - startMinutes
        if (totalMinutes <= 0) return fullDayKcal

        val currentMinutes = local.hour * 60 + local.minute
        val elapsedMinutes = when {
            currentMinutes <= startMinutes -> 0
            currentMinutes >= endMinutes -> totalMinutes
            else -> currentMinutes - startMinutes
        }
        val fraction = elapsedMinutes.toDouble() / totalMinutes.toDouble()
        return (fullDayKcal * fraction).roundToInt().coerceIn(0, fullDayKcal)
    }

    private fun offsetId(offsetMinutes: Int): String {
        val sign = if (offsetMinutes >= 0) "+" else "-"
        val abs = kotlin.math.abs(offsetMinutes)
        val hours = abs / 60
        val minutes = abs % 60
        return "GMT$sign${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }
}
