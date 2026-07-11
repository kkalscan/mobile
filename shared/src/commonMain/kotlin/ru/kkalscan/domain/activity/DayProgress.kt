package ru.kkalscan.domain.activity

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DayProgress {
    fun fractionNow(timeZone: TimeZone = TimeZone.currentSystemDefault()): Double {
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val minutes = now.hour * 60 + now.minute
        return (minutes / (24.0 * 60.0)).coerceIn(0.0, 1.0)
    }
}
