package ru.kkalscan.data.repository

actual fun currentTimezoneOffsetMinutes(): Int =
    java.time.ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds / 60
