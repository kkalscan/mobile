package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
actual fun MaestroNavigationBridge(
    onOpenScan: () -> Unit,
    onScanBack: () -> Unit,
) {
    DisposableEffect(onOpenScan, onScanBack) {
        val openBtn = document.getElementById("maestro-open-scan")
        val backBtn = document.getElementById("maestro-scan-back")
        val openHandler: (Event) -> Unit = { onOpenScan() }
        val backHandler: (Event) -> Unit = { onScanBack() }
        openBtn?.addEventListener("click", openHandler)
        backBtn?.addEventListener("click", backHandler)
        onDispose {
            openBtn?.removeEventListener("click", openHandler)
            backBtn?.removeEventListener("click", backHandler)
        }
    }
}
