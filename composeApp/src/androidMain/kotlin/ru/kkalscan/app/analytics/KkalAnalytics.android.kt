package ru.kkalscan.app.analytics

import android.app.Application
import android.content.Context
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import ru.kkalscan.analytics.AnalyticsEvents

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
        reportAction(AnalyticsEvents.APP_LAUNCH)
    }

    actual fun reportFeatureOpen(feature: String) {
        reportAction(AnalyticsEvents.FEATURE_OPEN, mapOf("feature" to feature))
    }

    actual fun reportAction(action: String, attributes: Map<String, String>) {
        if (!enabled) return

        AppMetrica.reportEvent(action, attributes.mapValues { it.value as Any })
    }
}
