package ru.kkalscan.app

import ru.kkalscan.data.IApiConfig

private object WebApiConfig : IApiConfig {
    override val apiBaseUrl: String = "http://localhost:8080/api/v1"
}

actual fun appApiConfig(): IApiConfig = WebApiConfig
