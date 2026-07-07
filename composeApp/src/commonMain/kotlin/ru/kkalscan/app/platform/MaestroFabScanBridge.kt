package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable

/** Positions `#maestro-fab-scan-photo` over the S action and wires fake/stub scan on wasm. */
@Composable
expect fun MaestroFabScanBridge(onFakeScanPhoto: () -> Unit)
