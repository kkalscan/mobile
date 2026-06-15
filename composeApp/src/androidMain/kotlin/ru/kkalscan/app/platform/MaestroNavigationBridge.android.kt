package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
actual fun MaestroNavigationBridge(
    onOpenScan: () -> Unit,
    onOpenProfile: () -> Unit,
    onScanBack: () -> Unit,
) = Unit
