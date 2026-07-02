package ru.kkalscan.app.navigation

import ru.kkalscan.app.components.AppTab
import ru.kkalscan.navigation.DeepLinkNavigationEffect
import ru.kkalscan.navigation.DeepLinkScreen
import ru.kkalscan.navigation.DeepLinkTab

fun DeepLinkNavigationEffect.toAppScreen(): AppScreen = when (screen) {
    DeepLinkScreen.Diary -> AppScreen.Diary
    DeepLinkScreen.Journal -> AppScreen.Journal
    DeepLinkScreen.Profile -> AppScreen.Profile
    DeepLinkScreen.Paywall -> AppScreen.Paywall
}

fun DeepLinkNavigationEffect.toAppTab(): AppTab? = when (tab) {
    DeepLinkTab.Today -> AppTab.Today
    DeepLinkTab.Journal -> AppTab.Journal
    DeepLinkTab.Profile -> AppTab.Profile
    null -> null
}
