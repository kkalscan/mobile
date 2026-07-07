package ru.kkalscan.domain.features

import ru.kkalscan.navigation.DeepLinkScreen
import ru.kkalscan.navigation.DeepLinkTab
import ru.kkalscan.navigation.resolveDeepLinkNavigation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end: feature search result → deeplink → navigation effect.
 */
class FeatureSearchDeepLinkIntegrationTest {

    @Test
    fun searchProfile_navigatesToProfile() {
        val item = FeatureSearchCatalog.search("профиль", limit = 5)
            .first { it.deeplink == "kkalscan://profile" }
        val effect = resolveDeepLinkNavigation(item.deeplink)
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Profile, effect.screen)
        assertEquals(DeepLinkTab.Profile, effect.tab)
    }

    @Test
    fun searchDiary_navigatesToToday() {
        val item = FeatureSearchCatalog.search("сегодня", limit = 5)
            .first { it.deeplink == "kkalscan://diary" }
        val effect = resolveDeepLinkNavigation(item.deeplink)
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Diary, effect.screen)
        assertEquals(DeepLinkTab.Today, effect.tab)
    }

    @Test
    fun searchJournal_navigatesToJournalTab() {
        val item = FeatureSearchCatalog.search("неделя", limit = 10)
            .first { it.deeplink == "kkalscan://journal" }
        val effect = resolveDeepLinkNavigation(item.deeplink)
        assertNotNull(effect)
        assertEquals(DeepLinkScreen.Journal, effect.screen)
        assertEquals(DeepLinkTab.Journal, effect.tab)
    }

    @Test
    fun searchFiber_navigatesToFiberSection() {
        val item = FeatureSearchCatalog.search("клетчатка", limit = 5)
            .first { it.deeplink == "kkalscan://journal/fiber" }
        val effect = resolveDeepLinkNavigation(item.deeplink)
        assertNotNull(effect)
        assertEquals("fiber", effect.journalScrollAnchor)
    }

    @Test
    fun searchScan_triggersScan() {
        val item = FeatureSearchCatalog.search("скан", limit = 5)
            .first { it.deeplink == "kkalscan://scan" }
        val effect = resolveDeepLinkNavigation(item.deeplink)
        assertNotNull(effect)
        assertTrue(effect.triggerScan)
    }

    @Test
    fun searchDescribeFood_opensDescribeSheet() {
        val item = FeatureSearchCatalog.search("описать", limit = 5)
            .first { it.deeplink == "kkalscan://describe-food" }
        val effect = resolveDeepLinkNavigation(item.deeplink)
        assertNotNull(effect)
        assertTrue(effect.openDescribeFood)
    }

    @Test
    fun searchFoodSearch_opensFoodSearchSheet() {
        val item = FeatureSearchCatalog.search("продукт", limit = 5)
            .first { it.deeplink == "kkalscan://food-search" }
        val effect = resolveDeepLinkNavigation(item.deeplink)
        assertNotNull(effect)
        assertTrue(effect.openFoodSearch)
    }

    @Test
    fun allCatalogItems_haveResolvableDeeplinks() {
        FeatureSearchCatalog.items.forEach { item ->
            assertNotNull(
                resolveDeepLinkNavigation(item.deeplink),
                "No navigation for ${item.id}: ${item.deeplink}",
            )
        }
    }
}
