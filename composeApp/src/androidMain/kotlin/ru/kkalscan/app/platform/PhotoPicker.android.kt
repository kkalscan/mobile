package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPhotoPicker(onPhotoPicked: (ByteArray?) -> Unit): () -> Unit =
    remember { { onPhotoPicked(null) } }
