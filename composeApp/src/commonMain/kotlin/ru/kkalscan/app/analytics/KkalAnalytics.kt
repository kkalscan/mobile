package ru.kkalscan.app.analytics

expect object KkalAnalytics {
    fun setDeviceId(deviceId: String)

    fun reportAppLaunch()

    fun reportFeatureOpen(feature: String)

    fun reportAction(action: String, attributes: Map<String, String> = emptyMap())
}
