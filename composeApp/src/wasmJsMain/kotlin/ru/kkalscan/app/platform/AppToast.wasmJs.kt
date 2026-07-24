package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberAppToast(): (String) -> Unit =
    remember {
        { message -> window.alert(message) }
    }
