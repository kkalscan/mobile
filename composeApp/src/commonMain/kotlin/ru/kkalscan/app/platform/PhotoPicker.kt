package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPhotoPicker(onPhotoPicked: (ByteArray?) -> Unit): () -> Unit
