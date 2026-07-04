package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun MaestroDevBridge(
    onStubScan: () -> Unit,
    onConfirmAdd: () -> Unit = {},
    onGramsPlus: () -> Unit = {},
    onGramsMinus: () -> Unit = {},
    onPortionHalf: () -> Unit = {},
    onPortionDouble: () -> Unit = {},
    onOpenFoodSearch: () -> Unit = {},
    onOpenDescribeFood: () -> Unit = {},
    onDescribeFoodDemo: () -> Unit = {},
    onFoodSearchDemo: () -> Unit = {},
    onFoodSearchAddFirst: () -> Unit = {},
    onDeepLinkProfile: () -> Unit = {},
    onDeepLinkJournal: () -> Unit = {},
    onDeepLinkDiary: () -> Unit = {},
    onFeatureSearchOpenFirst: () -> Unit = {},
)
