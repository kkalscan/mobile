package ru.kkalscan.analytics

/**
 * Single source of truth for AppMetrica custom event names.
 * Keep in sync with [mobile/docs/appmetrica-events.md].
 */
object AnalyticsEvents {
    const val APP_LAUNCH = "app_launch"
    const val FEATURE_OPEN = "feature_open"

    const val SCAN_OPEN = "scan_open"
    const val SCAN_RETRY = "scan_retry"
    const val PHOTO_SELECTED = "photo_selected"
    const val PHOTO_PICKER_CANCEL = "photo_picker_cancel"
    const val PHOTO_SCAN = "photo_scan"
    const val DESCRIBE_FOOD_OPEN = "describe_food_open"
    const val DESCRIBE_TEXT_SCAN = "describe_text_scan"
    const val DESCRIBE_FOOD_RECOGNIZED = "describe_food_recognized"
    const val SCAN_SUCCESS = "scan_success"
    const val SCAN_ERROR = "scan_error"
    const val FIRST_SCAN_SUCCESS = "first_scan_success"
    const val SECOND_SCAN_SUCCESS = "second_scan_success"
    const val THIRD_SCAN_SUCCESS = "third_scan_success"
    const val ADD_TO_DIARY = "add_to_diary"
    const val ADD_TO_DIARY_FAILED = "add_to_diary_failed"

    const val LIMIT_HIT = "limit_hit"
    const val PAYWALL_SHOWN = "paywall_shown"
    const val PAYWALL_BACK = "paywall_back"
    const val PRO_CLICK = "pro_click"
    const val SUBSCRIPTION_START = "subscription_start"

    const val AD_OFFER_SHOWN = "ad_offer_shown"
    const val AD_BONUS_CLICK = "ad_bonus_click"
    const val AD_WATCH_COMPLETE = "ad_watch_complete"
    const val AD_BONUS_FAILED = "ad_bonus_failed"

    const val BUG_REPORT_OPEN = "bug_report_open"
    const val BUG_REPORT_SUBMIT = "bug_report_submit"
    const val DIETITIAN_INSIGHT_CLICK = "dietitian_insight_click"

    const val FEATURE_SEARCH_OPEN = "feature_search_open"
    const val FEATURE_SEARCH_QUERY = "feature_search_query"
    const val FEATURE_SEARCH_FOOD_INTENT = "feature_search_food_intent"
    const val DEEPLINK_OPEN = "deeplink_open"

    const val DAY_1_RETURN = "day_1_return"
    const val DAY_7_RETURN = "day_7_return"

    /** Dev / Maestro only — not required in production funnels. */
    const val DEV_STUB_SCAN = "dev_stub_scan"

    const val FAB_ATTENTION_SHOWN = "fab_attention_shown"

    /** All events emitted by the app (excluding AppMetrica built-in install). */
    val allImplemented: Set<String> = setOf(
        APP_LAUNCH,
        FEATURE_OPEN,
        SCAN_OPEN,
        SCAN_RETRY,
        PHOTO_SELECTED,
        PHOTO_PICKER_CANCEL,
        PHOTO_SCAN,
        DESCRIBE_FOOD_OPEN,
        DESCRIBE_TEXT_SCAN,
        DESCRIBE_FOOD_RECOGNIZED,
        SCAN_SUCCESS,
        SCAN_ERROR,
        FIRST_SCAN_SUCCESS,
        SECOND_SCAN_SUCCESS,
        THIRD_SCAN_SUCCESS,
        ADD_TO_DIARY,
        ADD_TO_DIARY_FAILED,
        LIMIT_HIT,
        PAYWALL_SHOWN,
        PAYWALL_BACK,
        PRO_CLICK,
        SUBSCRIPTION_START,
        AD_OFFER_SHOWN,
        AD_BONUS_CLICK,
        AD_WATCH_COMPLETE,
        AD_BONUS_FAILED,
        BUG_REPORT_OPEN,
        BUG_REPORT_SUBMIT,
        DIETITIAN_INSIGHT_CLICK,
        FEATURE_SEARCH_OPEN,
        FEATURE_SEARCH_QUERY,
        FEATURE_SEARCH_FOOD_INTENT,
        DEEPLINK_OPEN,
        DAY_1_RETURN,
        DAY_7_RETURN,
        DEV_STUB_SCAN,
        FAB_ATTENTION_SHOWN,
    )

    /** Minimum set for RuStore ad / monetization readiness (test-plan §7). */
    val adsMonetizationRequired: Set<String> = setOf(
        FIRST_SCAN_SUCCESS,
        SECOND_SCAN_SUCCESS,
        THIRD_SCAN_SUCCESS,
        LIMIT_HIT,
        SCAN_ERROR,
        PHOTO_PICKER_CANCEL,
        ADD_TO_DIARY_FAILED,
        AD_OFFER_SHOWN,
        AD_BONUS_CLICK,
        AD_WATCH_COMPLETE,
        AD_BONUS_FAILED,
        PAYWALL_SHOWN,
        PAYWALL_BACK,
        PRO_CLICK,
        SUBSCRIPTION_START,
        DAY_1_RETURN,
        DAY_7_RETURN,
        DESCRIBE_TEXT_SCAN,
        DESCRIBE_FOOD_OPEN,
    )

    val scanFunnel: List<String> = listOf(
        SCAN_OPEN,
        PHOTO_SELECTED,
        PHOTO_SCAN,
        SCAN_SUCCESS,
        ADD_TO_DIARY,
    )

    val describeFunnel: List<String> = listOf(
        DESCRIBE_FOOD_OPEN,
        DESCRIBE_TEXT_SCAN,
        SCAN_SUCCESS,
        DESCRIBE_FOOD_RECOGNIZED,
        ADD_TO_DIARY,
    )

    val monetizationFunnel: List<String> = listOf(
        THIRD_SCAN_SUCCESS,
        LIMIT_HIT,
        PAYWALL_SHOWN,
        AD_OFFER_SHOWN,
        AD_BONUS_CLICK,
        AD_WATCH_COMPLETE,
        PRO_CLICK,
        SUBSCRIPTION_START,
    )
}
