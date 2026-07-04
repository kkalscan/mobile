package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
actual fun PlatformForegroundEffect(onForeground: () -> Unit) {
    val latestOnForeground by rememberUpdatedState(onForeground)
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { latestOnForeground() }
        window.addEventListener("focus", listener)
        onDispose { window.removeEventListener("focus", listener) }
    }
}
