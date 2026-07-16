package ru.kkalscan.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DeepLinkActionTest {

    @Test
    fun resolveDeepLink_profile() {
        assertIs<DeepLinkAction.Profile>(resolveDeepLink("kkalscan://profile"))
    }

    @Test
    fun resolveDeepLink_journalFiber() {
        val action = resolveDeepLink("kkalscan://journal/fiber")
        assertIs<DeepLinkAction.JournalSection>(action)
        assertEquals("fiber", action.journalScrollAnchor())
    }

    @Test
    fun resolveDeepLink_legacyFoodSearch_opensDescribeFood() {
        assertIs<DeepLinkAction.DescribeFood>(resolveDeepLink("kkalscan://food_search"))
        assertIs<DeepLinkAction.DescribeFood>(resolveDeepLink("kkalscan://food-search"))
    }

    @Test
    fun resolveDeepLink_describeFood() {
        assertIs<DeepLinkAction.DescribeFood>(resolveDeepLink("kkalscan://describe-food"))
        assertIs<DeepLinkAction.DescribeFood>(resolveDeepLink("kkalscan://describe_food"))
    }

    @Test
    fun resolveDeepLink_invalidHost() {
        assertNull(resolveDeepLink("kkalscan://settings"))
    }
}
