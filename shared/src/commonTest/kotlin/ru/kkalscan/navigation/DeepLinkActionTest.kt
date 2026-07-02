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
    fun resolveDeepLink_foodSearchUnderscore() {
        assertIs<DeepLinkAction.FoodSearch>(resolveDeepLink("kkalscan://food_search"))
    }

    @Test
    fun resolveDeepLink_invalidHost() {
        assertNull(resolveDeepLink("kkalscan://settings"))
    }
}
