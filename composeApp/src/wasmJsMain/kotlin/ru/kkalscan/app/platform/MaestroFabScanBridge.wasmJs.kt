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
        val tapScanBtn = document.getElementById("maestro-tap-fab-scan")
        val fakeHandler: (Event) -> Unit = { event ->
            if (useWasmFakeApi()) {
                event.preventDefault()
                event.stopPropagation()
                onFakeScanPhoto()
            }
        }
        val tapScanHandler: (Event) -> Unit = { MaestroFabController.onTapFabScan?.invoke() }
        label?.addEventListener("click", fakeHandler, true)
        tapScanBtn?.addEventListener("click", tapScanHandler)

        val resizeHandler: (Event) -> Unit = {
            if (document.getElementById("maestro-fab-actions-hook")?.textContent == "diary-fab-actions-3") {
                positionFabScanOverlay()
            }
        }
        window.addEventListener("resize", resizeHandler)

        onDispose {
            label?.removeEventListener("click", fakeHandler, true)
            tapScanBtn?.removeEventListener("click", tapScanHandler)
            window.removeEventListener("resize", resizeHandler)
        }
    }
}

private fun positionFabScanTapTarget(element: HTMLElement, leftPx: Double, topPx: Double) {
    element.style.position = "fixed"
    element.style.left = "${leftPx}px"
    element.style.top = "${topPx}px"
    element.style.width = "52px"
    element.style.height = "52px"
}

internal fun positionFabScanOverlay() {
    val mainFab = document.getElementById("maestro-tap-main-fab") as? HTMLElement ?: return
    val scanLabel = document.getElementById("maestro-fab-scan-photo") as? HTMLElement ?: return
    val tapScanBtn = document.getElementById("maestro-tap-fab-scan") as? HTMLElement ?: return
    val rect = mainFab.getBoundingClientRect()
    val slots = computeFabActionSlots(
        mainFabLeft = rect.left,
        mainFabTop = rect.top,
        mainFabWidth = rect.width,
    )
    val scan = slots[2]
    positionFabScanTapTarget(scanLabel, scan.leftPx, scan.topPx)
    positionFabScanTapTarget(tapScanBtn, scan.leftPx, scan.topPx)
}

internal fun setFabScanOverlayVisible(visible: Boolean) {
    val scanLabel = document.getElementById("maestro-fab-scan-photo") as? HTMLElement ?: return
    val tapScanBtn = document.getElementById("maestro-tap-fab-scan") as? HTMLElement ?: return
    if (visible) {
        positionFabScanOverlay()
        listOf(scanLabel, tapScanBtn).forEach { target ->
            target.style.setProperty("pointer-events", "auto")
            target.style.setProperty("opacity", "0.15")
            target.style.setProperty("z-index", "12")
        }
    } else {
        listOf(scanLabel, tapScanBtn).forEach { target ->
            target.style.setProperty("pointer-events", "none")
            target.style.setProperty("opacity", "0")
        }
    }
}
