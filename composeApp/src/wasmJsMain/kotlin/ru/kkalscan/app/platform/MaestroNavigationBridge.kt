package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
actual fun MaestroNavigationBridge(
    onOpenScan: () -> Unit,
    onOpenProfile: () -> Unit,
    onScanBack: () -> Unit,
) {
    DisposableEffect(onOpenScan, onOpenProfile, onScanBack) {
        val openBtn = document.getElementById("maestro-open-scan")
        val profileBtn = document.getElementById("maestro-open-profile")
        val backBtn = document.getElementById("maestro-scan-back")
        val openHandler: (Event) -> Unit = { onOpenScan() }
        val profileHandler: (Event) -> Unit = { onOpenProfile() }
        val backHandler: (Event) -> Unit = { onScanBack() }
        openBtn?.addEventListener("click", openHandler)
        profileBtn?.addEventListener("click", profileHandler)
        backBtn?.addEventListener("click", backHandler)
        onDispose {
            openBtn?.removeEventListener("click", openHandler)
            profileBtn?.removeEventListener("click", profileHandler)
            backBtn?.removeEventListener("click", backHandler)
        }
    }
}
