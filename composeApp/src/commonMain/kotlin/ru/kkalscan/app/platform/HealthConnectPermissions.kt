package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberHealthConnectPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit
