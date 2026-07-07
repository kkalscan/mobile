package ru.kkalscan.app.platform

internal data class FabActionSlot(val leftPx: Double, val topPx: Double)

/** Positions for describe / workout / scan FAB actions above the main FAB (left → right). */
internal fun computeFabActionSlots(
    mainFabLeft: Double,
    mainFabTop: Double,
    mainFabWidth: Double,
    buttonSizePx: Double = 52.0,
    gapPx: Double = 12.0,
    alignEnd: Boolean = true,
): List<FabActionSlot> {
    val rowWidth = buttonSizePx * 3 + gapPx * 2
    val rowLeft = if (alignEnd) {
        mainFabLeft + mainFabWidth - rowWidth
    } else {
        mainFabLeft + mainFabWidth / 2 - rowWidth / 2
    }
    val rowTop = mainFabTop - gapPx - buttonSizePx
    return listOf(
        FabActionSlot(rowLeft, rowTop),
        FabActionSlot(rowLeft + buttonSizePx + gapPx, rowTop),
        FabActionSlot(rowLeft + 2 * (buttonSizePx + gapPx), rowTop),
    )
}
