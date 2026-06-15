package ru.kkalscan.util

actual fun kkalLog(tag: String, message: String) {
    println("[$tag] $message")
}
