package ru.kkalscan.app.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    if (bytes.isEmpty()) return null
    val image = Image.makeFromEncoded(bytes) ?: return null
    return image.toComposeImageBitmap()
}
