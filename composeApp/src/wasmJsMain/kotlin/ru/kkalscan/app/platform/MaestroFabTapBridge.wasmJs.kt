package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
actual fun MaestroFabTapBridge() {
    DisposableEffect(Unit) {
        val btn = document.getElementById("maestro-tap-main-fab")
        val handler: (Event) -> Unit = { MaestroFabController.onTapMainFab?.invoke() }
        btn?.addEventListener("click", handler)
        onDispose {
            btn?.removeEventListener("click", handler)
        }
    }
}
