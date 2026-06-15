package ru.kkalscan.data.repository

@JsFun("() => -new Date().getTimezoneOffset()")
private external fun readTimezoneOffsetMinutes(): Int

actual fun currentTimezoneOffsetMinutes(): Int = readTimezoneOffsetMinutes()
