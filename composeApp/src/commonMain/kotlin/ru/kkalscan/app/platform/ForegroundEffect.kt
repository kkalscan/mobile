package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

/**
 * Invokes [onForeground] every time the app returns to the foreground
 * (Android: on ON_START after the initial start; web: on window focus).
 * Used to roll the "today" diary over when the app has been backgrounded
 * across a calendar-day boundary.
 */
@Composable
expect fun PlatformForegroundEffect(onForeground: () -> Unit)
