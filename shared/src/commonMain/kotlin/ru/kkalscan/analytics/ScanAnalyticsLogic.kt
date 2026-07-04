package ru.kkalscan.analytics

data class AnalyticsEvent(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
)

object ScanAnalyticsLogic {

    fun scanSuccessEvents(scansLeft: Int?): List<AnalyticsEvent> = buildList {
        add(AnalyticsEvent(AnalyticsEvents.SCAN_SUCCESS))
        when (scansLeft) {
            3 -> add(AnalyticsEvent(AnalyticsEvents.FIRST_SCAN_SUCCESS))
            2 -> add(AnalyticsEvent(AnalyticsEvents.SECOND_SCAN_SUCCESS))
            1 -> add(AnalyticsEvent(AnalyticsEvents.THIRD_SCAN_SUCCESS))
        }
    }

    fun scanOutcomeEvents(
        scansLeft: Int?,
        limitHit: Boolean,
        hasResult: Boolean,
        errorMessage: String?,
    ): List<AnalyticsEvent> = when {
        limitHit -> listOf(
            AnalyticsEvent(
                AnalyticsEvents.LIMIT_HIT,
                mapOf("scans_left" to scansLeft.orEmptyAnalyticsValue()),
            ),
        )
        hasResult -> scanSuccessEvents(scansLeft)
        errorMessage != null -> listOf(
            AnalyticsEvent(
                AnalyticsEvents.SCAN_ERROR,
                mapOf("reason" to errorMessage.analyticsReason()),
            ),
        )
        else -> emptyList()
    }

    private fun Int?.orEmptyAnalyticsValue(): String = this?.toString() ?: "unknown"

    private fun String.analyticsReason(): String = takeIf { it.isNotBlank() } ?: "unknown"
}
