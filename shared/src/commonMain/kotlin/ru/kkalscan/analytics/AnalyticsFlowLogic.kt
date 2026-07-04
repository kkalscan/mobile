package ru.kkalscan.analytics

/**
 * Pure functions describing analytics event sequences for user flows.
 * Used by UI layer and verified by unit tests before ad launch.
 */
object AnalyticsFlowLogic {

    fun photoScanHappyPath(scansLeftAfterSuccess: Int): List<String> = buildList {
        add(AnalyticsEvents.SCAN_OPEN)
        add(AnalyticsEvents.PHOTO_SELECTED)
        add(AnalyticsEvents.PHOTO_SCAN)
        addAll(ScanAnalyticsLogic.scanSuccessEvents(scansLeftAfterSuccess).map { it.name })
        add(AnalyticsEvents.ADD_TO_DIARY)
    }

    fun describeFoodHappyPath(scansLeftAfterSuccess: Int): List<String> = buildList {
        add(AnalyticsEvents.DESCRIBE_FOOD_OPEN)
        add(AnalyticsEvents.DESCRIBE_TEXT_SCAN)
        addAll(ScanAnalyticsLogic.scanSuccessEvents(scansLeftAfterSuccess).map { it.name })
        add(AnalyticsEvents.DESCRIBE_FOOD_RECOGNIZED)
        add(AnalyticsEvents.ADD_TO_DIARY)
    }

    fun photoScanLimitHitPath(): List<String> = listOf(
        AnalyticsEvents.SCAN_OPEN,
        AnalyticsEvents.PHOTO_SELECTED,
        AnalyticsEvents.PHOTO_SCAN,
        AnalyticsEvents.LIMIT_HIT,
        AnalyticsEvents.PAYWALL_SHOWN,
        AnalyticsEvents.AD_OFFER_SHOWN,
    )

    fun describeLimitHitPath(): List<String> = listOf(
        AnalyticsEvents.DESCRIBE_FOOD_OPEN,
        AnalyticsEvents.DESCRIBE_TEXT_SCAN,
        AnalyticsEvents.LIMIT_HIT,
        AnalyticsEvents.PAYWALL_SHOWN,
        AnalyticsEvents.AD_OFFER_SHOWN,
    )

    fun rewardedAdSuccessPath(): List<String> = listOf(
        AnalyticsEvents.AD_BONUS_CLICK,
        AnalyticsEvents.AD_WATCH_COMPLETE,
    )

    fun rewardedAdFailedPath(reason: String): List<AnalyticsEvent> = listOf(
        AnalyticsEvent(AnalyticsEvents.AD_BONUS_CLICK),
        AnalyticsEvent(AnalyticsEvents.AD_BONUS_FAILED, mapOf("reason" to reason)),
    )

    fun proPurchasePath(): List<String> = listOf(
        AnalyticsEvents.PRO_CLICK,
        AnalyticsEvents.SUBSCRIPTION_START,
    )

    fun paywallExitPath(): List<String> = listOf(
        AnalyticsEvents.PAYWALL_BACK,
    )

    fun addToDiaryFailedPath(reason: String): List<AnalyticsEvent> = listOf(
        AnalyticsEvent(AnalyticsEvents.ADD_TO_DIARY),
        AnalyticsEvent(AnalyticsEvents.ADD_TO_DIARY_FAILED, mapOf("reason" to reason)),
    )

    fun featureSearchQueryEvent(query: String, resultsCount: Int): AnalyticsEvent =
        AnalyticsEvent(
            AnalyticsEvents.FEATURE_SEARCH_QUERY,
            mapOf(
                "query" to query.take(200),
                "query_length" to query.length.toString(),
                "results" to resultsCount.toString(),
                "empty_query" to query.isBlank().toString(),
            ),
        )

    fun deeplinkEvent(link: String): AnalyticsEvent =
        AnalyticsEvent(AnalyticsEvents.DEEPLINK_OPEN, mapOf("link" to link))

    fun paywallScreenEvents(): List<String> = listOf(
        AnalyticsEvents.FEATURE_OPEN,
        AnalyticsEvents.PAYWALL_SHOWN,
        AnalyticsEvents.AD_OFFER_SHOWN,
    )

    fun adWatchCompleteEvent(scansLeft: Int): AnalyticsEvent =
        AnalyticsEvent(
            AnalyticsEvents.AD_WATCH_COMPLETE,
            mapOf("scans_left" to scansLeft.toString()),
        )
}
