package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

/** Wires `#maestro-tap-main-fab` DOM button to [MaestroFabController] (wasm Maestro only). */
@Composable
expect fun MaestroFabTapBridge()
