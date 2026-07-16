package ru.kkalscan.navigation

sealed class DeepLinkAction {
    data object Diary : DeepLinkAction()
    data object Journal : DeepLinkAction()
    data class JournalSection(val section: String) : DeepLinkAction()
    data object Profile : DeepLinkAction()
    data object Scan : DeepLinkAction()
    data object DescribeFood : DeepLinkAction()
    data object Paywall : DeepLinkAction()
}

fun resolveDeepLink(raw: String): DeepLinkAction? {
    val route = parseDeepLink(raw) ?: return null
    return when (route.host) {
        "diary", "today" -> DeepLinkAction.Diary
        "journal" -> when (route.path) {
            "fiber" -> DeepLinkAction.JournalSection("fiber")
            "dietitian" -> DeepLinkAction.JournalSection("dietitian")
            else -> DeepLinkAction.Journal
        }
        "profile" -> DeepLinkAction.Profile
        "scan" -> DeepLinkAction.Scan
        // Legacy catalog search → text describe (no product catalog).
        "food-search", "food_search", "describe-food", "describe_food" -> DeepLinkAction.DescribeFood
        "paywall", "pro" -> DeepLinkAction.Paywall
        else -> null
    }
}

fun DeepLinkAction.journalScrollAnchor(): String? = when (this) {
    is DeepLinkAction.JournalSection -> section
    else -> null
}
