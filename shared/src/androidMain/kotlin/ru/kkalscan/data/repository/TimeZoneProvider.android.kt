package ru.kkalscan.data.repository

actual fun currentTimezoneOffsetMinutes(): Int =
    (java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000)
