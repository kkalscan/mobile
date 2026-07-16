package ru.kkalscan.navigation

enum class DeepLinkScreen {
    Diary,
    Journal,
    Profile,
    Paywall,
}

enum class DeepLinkTab {
    Today,
    Journal,
    Profile,
}

data class DeepLinkNavigationEffect(
    val screen: DeepLinkScreen,
    val tab: DeepLinkTab? = null,
    val journalScrollAnchor: String? = null,
    val openDescribeFood: Boolean = false,
    val triggerScan: Boolean = false,
)

fun DeepLinkAction.toNavigationEffect(): DeepLinkNavigationEffect = when (this) {
    DeepLinkAction.Diary -> DeepLinkNavigationEffect(
        screen = DeepLinkScreen.Diary,
        tab = DeepLinkTab.Today,
    )
    DeepLinkAction.Journal -> DeepLinkNavigationEffect(
        screen = DeepLinkScreen.Journal,
        tab = DeepLinkTab.Journal,
    )
    is DeepLinkAction.JournalSection -> DeepLinkNavigationEffect(
        screen = DeepLinkScreen.Journal,
        tab = DeepLinkTab.Journal,
        journalScrollAnchor = section,
    )
    DeepLinkAction.Profile -> DeepLinkNavigationEffect(
        screen = DeepLinkScreen.Profile,
        tab = DeepLinkTab.Profile,
    )
    DeepLinkAction.Scan -> DeepLinkNavigationEffect(
        screen = DeepLinkScreen.Diary,
        tab = DeepLinkTab.Today,
        triggerScan = true,
    )
    DeepLinkAction.DescribeFood -> DeepLinkNavigationEffect(
        screen = DeepLinkScreen.Diary,
        tab = DeepLinkTab.Today,
        openDescribeFood = true,
    )
    DeepLinkAction.Paywall -> DeepLinkNavigationEffect(
        screen = DeepLinkScreen.Paywall,
        tab = null,
    )
}

fun resolveDeepLinkNavigation(raw: String): DeepLinkNavigationEffect? =
    resolveDeepLink(raw)?.toNavigationEffect()
