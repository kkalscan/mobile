package ru.kkalscan.data

interface IApiConfig {
    val apiBaseUrl: String

    val webBaseUrl: String
        get() = apiBaseUrl.removeSuffix("/api/v1").trimEnd('/')
}
