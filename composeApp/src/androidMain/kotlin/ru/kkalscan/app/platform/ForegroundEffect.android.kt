package ru.kkalscan.app.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

@Composable
actual fun PlatformForegroundEffect(onForeground: () -> Unit) {
    val latestOnForeground by rememberUpdatedState(onForeground)
    val lifecycleOwner = LocalContext.current.findLifecycleOwner()

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner?.lifecycle ?: return@DisposableEffect onDispose { }

        // Adding an observer replays events up to the current state, so the first
        // ON_START fires for the initial launch — skip it and only react to real
        // background -> foreground transitions afterwards.
        var initialStartSeen = false
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (initialStartSeen) {
                    latestOnForeground()
                } else {
                    initialStartSeen = true
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is LifecycleOwner) return context
        context = context.baseContext
    }
    return null
}
