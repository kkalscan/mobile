package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberAppToast(): (String) -> Unit
