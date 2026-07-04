package ru.kkalscan.app.platform

import kotlinx.browser.window

actual fun isStoreScreenshotMode(): Boolean =
    window.location.search.contains("screenshot=1")
