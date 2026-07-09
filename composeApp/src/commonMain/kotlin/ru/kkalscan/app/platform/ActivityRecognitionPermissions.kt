package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberActivityRecognitionPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit
