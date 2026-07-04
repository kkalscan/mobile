package ru.kkalscan.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinkNavigationTest {

    @Test
    fun diary_deeplink_navigatesToToday() {
        val effect = resolveDeepLinkNavigation("kkalscan://diary")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Diary, effect.screen)
        assertEquals(DeepLinkTab.Today, effect.tab)
        assertNull(effect.journalScrollAnchor)
        assertFalse(effect.openFoodSearch)
        assertFalse(effect.openDescribeFood)
        assertFalse(effect.triggerScan)
    }

    @Test
    fun today_alias_navigatesToToday() {
        val effect = resolveDeepLinkNavigation("kkalscan://today")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Diary, effect.screen)
        assertEquals(DeepLinkTab.Today, effect.tab)
    }

    @Test
    fun journal_deeplink_navigatesToJournalTab() {
        val effect = resolveDeepLinkNavigation("kkalscan://journal")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Journal, effect.screen)
        assertEquals(DeepLinkTab.Journal, effect.tab)
        assertNull(effect.journalScrollAnchor)
    }

    @Test
    fun journalFiber_deeplink_scrollsToFiberChart() {
        val effect = resolveDeepLinkNavigation("kkalscan://journal/fiber")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Journal, effect.screen)
        assertEquals(DeepLinkTab.Journal, effect.tab)
        assertEquals("fiber", effect.journalScrollAnchor)
    }

    @Test
    fun journalDietitian_deeplink_scrollsToDietitian() {
        val effect = resolveDeepLinkNavigation("kkalscan://journal/dietitian")
        assertNotNull(effect)
        assertEquals("dietitian", effect.journalScrollAnchor)
    }

    @Test
    fun profile_deeplink_navigatesToProfileTab() {
        val effect = resolveDeepLinkNavigation("kkalscan://profile")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Profile, effect.screen)
        assertEquals(DeepLinkTab.Profile, effect.tab)
    }

    @Test
    fun scan_deeplink_triggersScanOnDiary() {
        val effect = resolveDeepLinkNavigation("kkalscan://scan")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Diary, effect.screen)
        assertEquals(DeepLinkTab.Today, effect.tab)
        assertTrue(effect.triggerScan)
    }

    @Test
    fun foodSearch_deeplink_opensFoodSearchSheet() {
        val effect = resolveDeepLinkNavigation("kkalscan://food-search")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Diary, effect.screen)
        assertTrue(effect.openFoodSearch)
        assertFalse(effect.openDescribeFood)
    }

    @Test
    fun describeFood_deeplink_opensDescribeSheet() {
        val effect = resolveDeepLinkNavigation("kkalscan://describe-food")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Diary, effect.screen)
        assertEquals(DeepLinkTab.Today, effect.tab)
        assertTrue(effect.openDescribeFood)
        assertFalse(effect.openFoodSearch)
    }

    @Test
    fun paywall_deeplink_navigatesWithoutTab() {
        val effect = resolveDeepLinkNavigation("kkalscan://paywall")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Paywall, effect.screen)
        assertNull(effect.tab)
    }

    @Test
    fun pro_alias_opensPaywall() {
        val effect = resolveDeepLinkNavigation("kkalscan://pro")
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Paywall, effect.screen)
    }

    @Test
    fun unknown_deeplink_returnsNull() {
        assertNull(resolveDeepLinkNavigation("kkalscan://unknown-screen"))
        assertNull(resolveDeepLinkNavigation(""))
    }
}
