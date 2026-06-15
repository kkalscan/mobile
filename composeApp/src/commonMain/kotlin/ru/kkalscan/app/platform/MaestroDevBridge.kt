package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun MaestroDevBridge(
    onStubScan: () -> Unit,
    onConfirmAdd: () -> Unit = {},
)
