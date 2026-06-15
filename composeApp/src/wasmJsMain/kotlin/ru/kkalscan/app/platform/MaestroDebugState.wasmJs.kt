package ru.kkalscan.app.platform

import kotlinx.browser.document

actual fun updateMaestroDebugState(text: String) {
    val hook = document.getElementById("maestro-scan-state")
        ?: run {
            val element = document.createElement("div")
            element.id = "maestro-scan-state"
            element.setAttribute("role", "status")
            element.setAttribute("aria-live", "polite")
            document.body?.appendChild(element)
            element
        }
    hook.textContent = text
}
