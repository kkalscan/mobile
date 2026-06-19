package ru.kkalscan.app.analytics

import android.app.Application
import android.content.Context
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

actual object KkalAnalytics {
    private var enabled = false

    fun init(context: Context, application: Application, apiKey: String) {
        if (enabled || apiKey.isBlank()) return

        val config = AppMetricaConfig.newConfigBuilder(apiKey).build()
        AppMetrica.activate(context.applicationContext, config)
        AppMetrica.enableActivityAutoTracking(application)
        enabled = true
    }

    actual fun setDeviceId(deviceId: String) {
        if (!enabled) return

        AppMetrica.setUserProfileID(deviceId)
    }

    actual fun reportAppLaunch() {
        reportAction("app_launch")
    }

    actual fun reportFeatureOpen(feature: String) {
        reportAction("feature_open", mapOf("feature" to feature))
    }

    actual fun reportAction(action: String, attributes: Map<String, String>) {
        if (!enabled) return

        AppMetrica.reportEvent(action, attributes.mapValues { it.value as Any })
    }
}
