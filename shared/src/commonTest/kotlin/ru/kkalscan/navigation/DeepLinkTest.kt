package ru.kkalscan.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeepLinkTest {

    @Test
    fun parseDeepLink_diary() {
        val route = parseDeepLink("kkalscan://diary")
        assertNotNull(route)
        assertEquals("diary", route.host)
        assertEquals("", route.path)
    }

    @Test
    fun parseDeepLink_journalSection() {
        val route = parseDeepLink("kkalscan://journal/fiber")
        assertNotNull(route)
        assertEquals("journal", route.host)
        assertEquals("fiber", route.path)
    }

    @Test
    fun parseDeepLink_pathWithoutScheme() {
        val route = parseDeepLink("/profile")
        assertNotNull(route)
        assertEquals("profile", route.host)
    }

    @Test
    fun parseDeepLink_hostOnlyWithoutScheme() {
        val route = parseDeepLink("journal")
        assertNotNull(route)
        assertEquals("journal", route.host)
    }

    @Test
    fun parseDeepLink_caseInsensitiveHost() {
        val route = parseDeepLink("kkalscan://Profile")
        assertNotNull(route)
        assertEquals("profile", route.host)
    }

    @Test
    fun parseDeepLink_invalid() {
        assertNull(parseDeepLink(""))
        assertNull(parseDeepLink("   "))
    }
}
