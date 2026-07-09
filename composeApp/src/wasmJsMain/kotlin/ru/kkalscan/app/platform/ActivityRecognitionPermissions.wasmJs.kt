package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberActivityRecognitionPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit =
    { onResult(false) }
