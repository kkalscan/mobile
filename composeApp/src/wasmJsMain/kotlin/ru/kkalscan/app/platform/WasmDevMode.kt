package ru.kkalscan.app.platform

import kotlinx.browser.window

internal fun useWasmFakeApi(): Boolean {
    val query = window.location.search
    if (query.contains("fake=0")) return false
    if (query.contains("fake=1")) return true
    val host = window.location.hostname
    return host == "localhost" || host == "127.0.0.1"
}
