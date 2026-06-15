package ru.kkalscan.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.theme.KkalScanColors

expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

@Composable
fun FoodPhotoThumbnail(
    photoBytes: ByteArray?,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val bitmap = remember(photoBytes) {
        photoBytes?.let { decodeImageBitmap(it) }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(KkalScanColors.Outline.copy(alpha = 0.15f)),
            contentScale = ContentScale.Crop,
        )
    } else {
        KkalIconBadge(label = fallbackLabel, modifier = modifier)
    }
}
