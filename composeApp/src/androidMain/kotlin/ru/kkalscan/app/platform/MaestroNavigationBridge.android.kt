package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
actual fun MaestroNavigationBridge(
    onOpenScan: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenJournal: () -> Unit,
    onScanBack: () -> Unit,
) = Unit
