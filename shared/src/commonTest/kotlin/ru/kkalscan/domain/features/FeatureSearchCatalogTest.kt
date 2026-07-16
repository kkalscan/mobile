package ru.kkalscan.domain.features

import kotlin.test.Test
import kotlin.test.assertTrue

class FeatureSearchCatalogTest {

    @Test
    fun search_profile_by_keyword() {
        val results = FeatureSearchCatalog.search("профиль", limit = 10)
        assertTrue(results.any { it.deeplink == "kkalscan://profile" })
    }

    @Test
    fun search_diary_by_keyword() {
        val results = FeatureSearchCatalog.search("дневник", limit = 10)
        assertTrue(results.any { it.deeplink == "kkalscan://diary" || it.deeplink == "kkalscan://journal" })
    }

    @Test
    fun emptyQuery_returnsNothing() {
        val results = FeatureSearchCatalog.query("", limit = 5)
        assertTrue(results.items.isEmpty())
        assertTrue(!results.popularFallback)
    }

    @Test
    fun unknownQuery_returnsPopularFallback() {
        val results = FeatureSearchCatalog.query("xyzunknown123", limit = 5)
        assertTrue(results.items.size >= 3)
        assertTrue(results.popularFallback)
    }

    @Test
    fun borscht_doesNotMatchFoodSearchFeature() {
        val result = FeatureSearchCatalog.query("борщ", limit = 10)
        assertTrue(result.popularFallback || result.items.none { it.id == "food_search" })
    }
}
