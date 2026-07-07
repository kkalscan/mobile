package ru.kkalscan.app.platform

import kotlinx.browser.document

internal actual fun updateMaestroFabState(expanded: Boolean) {
    val hook = document.getElementById("maestro-fab-hook") ?: return
    hook.textContent = if (expanded) "diary-fab-expanded" else "diary-fab-collapsed"
    document.getElementById("maestro-fab-actions-hook")?.textContent =
        if (expanded) "diary-fab-actions-3" else "diary-fab-actions-0"
    document.getElementById("maestro-fab-main-hook")?.textContent = "diary-fab-main-count-1"
    setFabScanOverlayVisible(expanded)
}
