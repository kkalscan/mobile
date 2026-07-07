package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPhotoPicker(
    source: PhotoPickSource,
    onPhotoPicked: (ByteArray?) -> Unit,
): () -> Unit =
    remember(onPhotoPicked) {
        if (useWasmFakeApi()) {
            { onPhotoPicked(devStubScanPhotoBytes() ?: byteArrayOf(1, 2, 3)) }
        } else {
            bindWasmPhotoInput(onPhotoPicked)
        }
    }
