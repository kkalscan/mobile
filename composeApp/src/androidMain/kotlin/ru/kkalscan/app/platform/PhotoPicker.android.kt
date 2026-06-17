package ru.kkalscan.app.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberPhotoPicker(
    source: PhotoPickSource,
    onPhotoPicked: (ByteArray?) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            onPhotoPicked(null)
            return@rememberLauncherForActivityResult
        }
        onPhotoPicked(readCompressedPhotoBytes(context, uri))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri.value
        pendingCameraUri.value = null
        if (!success || uri == null) {
            onPhotoPicked(null)
            return@rememberLauncherForActivityResult
        }
        onPhotoPicked(readCompressedPhotoBytes(context, uri))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            onPhotoPicked(null)
            return@rememberLauncherForActivityResult
        }
        val uri = createCameraImageUri(context)
        if (uri == null) {
            onPhotoPicked(null)
            return@rememberLauncherForActivityResult
        }
        pendingCameraUri.value = uri
        cameraLauncher.launch(uri)
    }

    return remember(source, galleryLauncher, cameraLauncher, permissionLauncher) {
        {
            when (source) {
                PhotoPickSource.Gallery -> {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }

                PhotoPickSource.Camera -> {
                    fun launchCamera() {
                        val uri = createCameraImageUri(context)
                        if (uri == null) {
                            onPhotoPicked(null)
                        } else {
                            pendingCameraUri.value = uri
                            cameraLauncher.launch(uri)
                        }
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        launchCamera()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }
}

private fun createCameraImageUri(context: Context): Uri? =
    runCatching {
        val file = File.createTempFile("scan_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }.getOrNull()
