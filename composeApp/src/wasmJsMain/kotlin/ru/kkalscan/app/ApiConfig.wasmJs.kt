package ru.kkalscan.app

import kotlinx.browser.window
import ru.kkalscan.data.IApiConfig

private object WebApiConfig : IApiConfig {
    override val apiBaseUrl: String
        get() = "${window.location.origin}/api/v1"
}

actual fun appApiConfig(): IApiConfig = WebApiConfig
