package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberProPaymentOpener(): (String) -> Unit
