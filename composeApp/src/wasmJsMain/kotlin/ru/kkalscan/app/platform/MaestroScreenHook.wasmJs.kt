package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.document

@Composable
actual fun MaestroScreenHook(screen: String) {
    LaunchedEffect(screen) {
        val hook = document.getElementById("maestro-hook") ?: run {
            val element = document.createElement("div")
            element.id = "maestro-hook"
            element.setAttribute("role", "status")
            element.setAttribute("aria-live", "polite")
            document.body?.appendChild(element)
            element
        }
        hook.setAttribute("aria-label", screen)
        hook.textContent = screen
    }
}
