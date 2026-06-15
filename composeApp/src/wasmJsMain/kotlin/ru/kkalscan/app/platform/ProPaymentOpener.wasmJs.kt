package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberProPaymentOpener(): (String) -> Unit =
    remember {
        { url -> window.open(url, "_blank") }
    }
