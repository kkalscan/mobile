package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
actual fun MaestroDevBridge(
    onStubScan: () -> Unit,
    onConfirmAdd: () -> Unit,
) {
    DisposableEffect(onStubScan, onConfirmAdd) {
        val stubBtn = document.getElementById("maestro-stub-scan")
        val confirmBtn = document.getElementById("maestro-confirm-add")
        val stubHandler: (Event) -> Unit = { onStubScan() }
        val confirmHandler: (Event) -> Unit = { onConfirmAdd() }
        stubBtn?.addEventListener("click", stubHandler)
        confirmBtn?.addEventListener("click", confirmHandler)
        onDispose {
            stubBtn?.removeEventListener("click", stubHandler)
            confirmBtn?.removeEventListener("click", confirmHandler)
        }
    }
}
