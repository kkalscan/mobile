package ru.kkalscan.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.document
import org.w3c.dom.events.Event

@Composable
actual fun MaestroDevBridge(
    onStubScan: () -> Unit,
    onConfirmAdd: () -> Unit,
    onGramsPlus: () -> Unit,
    onGramsMinus: () -> Unit,
    onPortionHalf: () -> Unit,
    onPortionDouble: () -> Unit,
) {
    DisposableEffect(onStubScan, onConfirmAdd, onGramsPlus, onGramsMinus, onPortionHalf, onPortionDouble) {
        val stubBtn = document.getElementById("maestro-stub-scan")
        val confirmBtn = document.getElementById("maestro-confirm-add")
        val plusBtn = document.getElementById("maestro-grams-plus")
        val minusBtn = document.getElementById("maestro-grams-minus")
        val halfBtn = document.getElementById("maestro-portion-half")
        val doubleBtn = document.getElementById("maestro-portion-double")
        val stubHandler: (Event) -> Unit = { onStubScan() }
        val confirmHandler: (Event) -> Unit = { onConfirmAdd() }
        val plusHandler: (Event) -> Unit = { onGramsPlus() }
        val minusHandler: (Event) -> Unit = { onGramsMinus() }
        val halfHandler: (Event) -> Unit = { onPortionHalf() }
        val doubleHandler: (Event) -> Unit = { onPortionDouble() }
        stubBtn?.addEventListener("click", stubHandler)
        confirmBtn?.addEventListener("click", confirmHandler)
        plusBtn?.addEventListener("click", plusHandler)
        minusBtn?.addEventListener("click", minusHandler)
        halfBtn?.addEventListener("click", halfHandler)
        doubleBtn?.addEventListener("click", doubleHandler)
        onDispose {
            stubBtn?.removeEventListener("click", stubHandler)
            confirmBtn?.removeEventListener("click", confirmHandler)
            plusBtn?.removeEventListener("click", plusHandler)
            minusBtn?.removeEventListener("click", minusHandler)
            halfBtn?.removeEventListener("click", halfHandler)
            doubleBtn?.removeEventListener("click", doubleHandler)
        }
    }
}
