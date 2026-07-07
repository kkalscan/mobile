package ru.kkalscan.app.platform

/** Lets wasm Maestro DOM hooks toggle the diary main FAB without reaching into Compose test tags. */
internal object MaestroFabController {
    var onTapMainFab: (() -> Unit)? = null
    var onTapFabScan: (() -> Unit)? = null
}
