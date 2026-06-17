package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPhotoPicker(
    source: PhotoPickSource = PhotoPickSource.Camera,
    onPhotoPicked: (ByteArray?) -> Unit,
): () -> Unit
