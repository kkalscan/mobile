package ru.kkalscan.app

import ru.kkalscan.AppDependencies
import ru.kkalscan.app.platform.useWasmFakeApi
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
