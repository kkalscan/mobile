package ru.kkalscan.app.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_EDGE_PX = 1920
private const val TARGET_MAX_BYTES = 500 * 1024

internal fun readCompressedPhotoBytes(context: Context, uri: Uri): ByteArray? {
    val raw = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return null

    return runCatching { compressRawPhoto(raw) }.getOrNull()
}

private fun compressRawPhoto(raw: ByteArray): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)

    val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
    val decoded = BitmapFactory.decodeByteArray(
        raw,
        0,
        raw.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: error("decode failed")

    val oriented = applyExifOrientation(raw, decoded)
    return compressToJpeg(oriented)
}

private fun calculateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sampleSize = 1
    while (max(width / sampleSize, height / sampleSize) > maxEdge) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun applyExifOrientation(raw: ByteArray, bitmap: Bitmap): Bitmap {
    val rotation = runCatching {
        ExifInterface(ByteArrayInputStream(raw)).rotationDegrees.toFloat()
    }.getOrElse { 0f }
    if (rotation == 0f) return bitmap

    val matrix = Matrix().apply { postRotate(rotation) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
}

private fun compressToJpeg(bitmap: Bitmap): ByteArray {
    val scaled = scaleDown(bitmap, MAX_EDGE_PX)
    val output = ByteArrayOutputStream()
    var quality = 88
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
    while (output.size() > TARGET_MAX_BYTES && quality > 45) {
        output.reset()
        quality -= 10
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
    }
    if (scaled !== bitmap) {
        scaled.recycle()
    }
    bitmap.recycle()
    return output.toByteArray()
}

private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    if (longest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longest
    val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}
