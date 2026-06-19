package ru.kkalscan.app.analytics

actual object KkalAnalytics {
    actual fun setDeviceId(deviceId: String) = Unit

    actual fun reportAppLaunch() = Unit

    actual fun reportFeatureOpen(feature: String) = Unit

    actual fun reportAction(action: String, attributes: Map<String, String>) = Unit
}
