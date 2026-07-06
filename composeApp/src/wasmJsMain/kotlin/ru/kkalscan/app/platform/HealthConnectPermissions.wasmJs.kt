package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberHealthConnectPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit =
    { onResult(true) }
