package ru.kkalscan.stats

import ru.kkalscan.domain.model.DiaryDay

object JournalDayMerger {
    fun mergeWeekWithTodayPatch(days: List<DiaryDay>, todayPatch: DiaryDay?): List<DiaryDay> {
        val patch = todayPatch ?: return days
        val hasToday = days.any { it.date == patch.date }
        return if (hasToday) {
            days.map { day -> if (day.date == patch.date) patch else day }
        } else {
            days + patch
        }
    }
}
