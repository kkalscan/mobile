package ru.kkalscan.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsEventsTest {

    @Test
    fun allImplemented_hasUniqueNames() {
        assertEquals(AnalyticsEvents.allImplemented.size, AnalyticsEvents.allImplemented.distinct().size)
    }

    @Test
    fun adsMonetizationRequired_isSubsetOfAllImplemented() {
        val missing = AnalyticsEvents.adsMonetizationRequired - AnalyticsEvents.allImplemented
        assertTrue(missing.isEmpty(), "Missing ads events in catalog: $missing")
    }

    @Test
    fun scanFunnel_eventsExistInCatalog() {
        val missing = AnalyticsEvents.scanFunnel.toSet() - AnalyticsEvents.allImplemented
        assertTrue(missing.isEmpty(), "Scan funnel events missing from catalog: $missing")
    }

    @Test
    fun describeFunnel_eventsExistInCatalog() {
        val missing = AnalyticsEvents.describeFunnel.toSet() - AnalyticsEvents.allImplemented
        assertTrue(missing.isEmpty(), "Describe funnel events missing from catalog: $missing")
    }

    @Test
    fun monetizationFunnel_eventsExistInCatalog() {
        val missing = AnalyticsEvents.monetizationFunnel.toSet() - AnalyticsEvents.allImplemented
        assertTrue(missing.isEmpty(), "Monetization funnel events missing from catalog: $missing")
    }

    @Test
    fun retentionEvents_inAdsRequiredSet() {
        assertTrue(AnalyticsEvents.DAY_1_RETURN in AnalyticsEvents.adsMonetizationRequired)
        assertTrue(AnalyticsEvents.DAY_7_RETURN in AnalyticsEvents.adsMonetizationRequired)
    }

    @Test
    fun adsMonetizationRequired_excludesDevOnlyEvents() {
        assertTrue(AnalyticsEvents.DEV_STUB_SCAN !in AnalyticsEvents.adsMonetizationRequired)
    }

    @Test
    fun allImplemented_containsAppLaunchAndFeatureOpen() {
        assertTrue(AnalyticsEvents.APP_LAUNCH in AnalyticsEvents.allImplemented)
        assertTrue(AnalyticsEvents.FEATURE_OPEN in AnalyticsEvents.allImplemented)
        assertTrue(AnalyticsEvents.ADD_TO_DIARY in AnalyticsEvents.allImplemented)
    }
}
