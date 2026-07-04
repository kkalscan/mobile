package ru.kkalscan.app.analytics

import ru.kkalscan.analytics.ScanAnalyticsLogic

internal object ScanAnalytics {

    fun reportScanSuccess(scansLeft: Int?) {
        ScanAnalyticsLogic.scanSuccessEvents(scansLeft).forEach { event ->
            KkalAnalytics.reportAction(event.name, event.attributes)
        }
    }

    fun reportScanOutcome(
        scansLeft: Int?,
        limitHit: Boolean,
        hasResult: Boolean,
        errorMessage: String?,
    ) {
        ScanAnalyticsLogic.scanOutcomeEvents(scansLeft, limitHit, hasResult, errorMessage).forEach { event ->
            KkalAnalytics.reportAction(event.name, event.attributes)
        }
    }
}

internal fun Int?.orEmptyAnalyticsValue(): String = this?.toString() ?: "unknown"

internal fun String?.analyticsReason(): String = this?.takeIf { it.isNotBlank() } ?: "unknown"
