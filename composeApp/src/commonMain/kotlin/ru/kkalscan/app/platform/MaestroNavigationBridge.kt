package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun MaestroNavigationBridge(
    onOpenScan: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenJournal: () -> Unit,
    onScanBack: () -> Unit,
)
