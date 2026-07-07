package ru.kkalscan.app.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.health.connect.client.PermissionController
import ru.kkalscan.data.health.healthConnectReadPermissions

@Composable
actual fun rememberHealthConnectPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val permissions = healthConnectReadPermissions()
    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        onResult(granted.containsAll(permissions))
    }
    return { launcher.launch(permissions) }
}
