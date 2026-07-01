package ru.kkalscan.app

import kotlinx.browser.window
import ru.kkalscan.AppDependencies
import ru.kkalscan.data.api.FakeKkalScanApi

actual fun createAppDependencies(): AppDependencies {
    val config = appApiConfig()
    return if (useWasmFakeApi()) {
        AppDependencies(
            apiConfig = config,
            api = FakeKkalScanApi(seedSampleWeek = true),
        )
    } else {
        AppDependencies(apiConfig = config)
    }
}

private fun useWasmFakeApi(): Boolean {
    val query = window.location.search
    if (query.contains("fake=0")) return false
    if (query.contains("fake=1")) return true
    val host = window.location.hostname
    return host == "localhost" || host == "127.0.0.1"
}
