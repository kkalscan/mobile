package ru.kkalscan.stats

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object WeekDates {
    private val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    fun today(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
        val now = Clock.System.now().toLocalDateTime(timeZone)
        return LocalDate(now.year, now.monthNumber, now.dayOfMonth)
    }

    fun mondayOf(date: LocalDate): LocalDate {
        val daysSinceMonday = daysSinceMonday(date.dayOfWeek)
        return date.minus(DatePeriod(days = daysSinceMonday))
    }

    fun weekFrom(monday: LocalDate): List<String> =
        (0..6).map { offset -> iso(monday.plus(DatePeriod(days = offset))) }

    fun iso(date: LocalDate): String {
        val m = date.monthNumber.toString().padStart(2, '0')
        val d = date.dayOfMonth.toString().padStart(2, '0')
        return "${date.year}-$m-$d"
    }

    fun parse(iso: String): LocalDate {
        val parts = iso.split("-")
        return LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    fun currentWeekStart(): String = iso(mondayOf(today()))

    fun formatWeekLabel(weekStartIso: String): String {
        val start = parse(weekStartIso)
        val end = start.plus(DatePeriod(days = 6))
        return "${start.dayOfMonth}.${start.monthNumber.toString().padStart(2, '0')} – " +
            "${end.dayOfMonth}.${end.monthNumber.toString().padStart(2, '0')}"
    }

    fun shortDayLabel(dateIso: String, weekStartIso: String): String {
        val index = weekFrom(parse(weekStartIso)).indexOf(dateIso).coerceIn(0, 6)
        return dayLabels[index]
    }

    fun isAfter(date: LocalDate, other: LocalDate): Boolean =
        date.year > other.year ||
            (date.year == other.year && date.monthNumber > other.monthNumber) ||
            (date.year == other.year && date.monthNumber == other.monthNumber && date.dayOfMonth > other.dayOfMonth)

    private fun daysSinceMonday(day: DayOfWeek): Int = when (day) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }
}
