package ru.kkalscan.util

import android.util.Log

actual fun kkalLog(tag: String, message: String) {
    Log.d(tag, message)
}
