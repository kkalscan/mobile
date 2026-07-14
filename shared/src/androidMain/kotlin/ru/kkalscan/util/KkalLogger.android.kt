package ru.kkalscan.util

import android.util.Log

actual fun kkalLog(tag: String, message: String) {
    try {
        Log.d(tag, message)
    } catch (_: RuntimeException) {
        // Android instrumented stubs throw in JVM unit tests without Robolectric.
        println("$tag: $message")
    }
}
