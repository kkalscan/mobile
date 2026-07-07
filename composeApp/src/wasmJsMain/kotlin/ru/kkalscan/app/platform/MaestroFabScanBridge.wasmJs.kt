package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

@Composable
actual fun MaestroFabScanBridge(onFakeScanPhoto: () -> Unit) {
    DisposableEffect(onFakeScanPhoto) {
        val label = document.getElementById("maestro-fab-scan-photo")
        val fakeHandler: (Event) -> Unit = { event ->
            if (useWasmFakeApi()) {
                event.preventDefault()
                event.stopPropagation()
                onFakeScanPhoto()
            }
        }
        label?.addEventListener("click", fakeHandler, true)

        val resizeHandler: (Event) -> Unit = {
            if (document.getElementById("maestro-fab-hook")?.textContent == "diary-fab-expanded") {
                positionFabScanOverlay()
            }
        }
        window.addEventListener("resize", resizeHandler)

        onDispose {
            label?.removeEventListener("click", fakeHandler, true)
            window.removeEventListener("resize", resizeHandler)
        }
    }
}

internal fun positionFabScanOverlay() {
    val mainFab = document.getElementById("maestro-tap-main-fab") as? HTMLElement ?: return
    val scanLabel = document.getElementById("maestro-fab-scan-photo") as? HTMLElement ?: return
    val rect = mainFab.getBoundingClientRect()
    val slots = computeFabActionSlots(
        mainFabLeft = rect.left,
        mainFabTop = rect.top,
        mainFabWidth = rect.width,
    )
    val scan = slots[2]
    scanLabel.style.position = "fixed"
    scanLabel.style.left = "${scan.leftPx}px"
    scanLabel.style.top = "${scan.topPx}px"
    scanLabel.style.width = "52px"
    scanLabel.style.height = "52px"
}

internal fun setFabScanOverlayVisible(visible: Boolean) {
    val scanLabel = document.getElementById("maestro-fab-scan-photo") as? HTMLElement ?: return
    if (visible) {
        positionFabScanOverlay()
        scanLabel.style.setProperty("pointer-events", "auto")
        scanLabel.style.setProperty("opacity", "0.15")
        scanLabel.style.setProperty("z-index", "12")
    } else {
        scanLabel.style.setProperty("pointer-events", "none")
        scanLabel.style.setProperty("opacity", "0")
    }
}
