package ru.kkalscan.app.components

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kkalscan.app.platform.FabActionSlot
import ru.kkalscan.app.platform.computeFabActionSlots

class FabActionOverlayLayoutTest {

    @Test
    fun computeFabActionSlots_placesScanRightmostAboveRightAlignedMainFab() {
        val slots = computeFabActionSlots(
            mainFabLeft = 314.0,
            mainFabTop = 200.0,
            mainFabWidth = 56.0,
        )
        assertEquals(
            listOf(
                FabActionSlot(190.0, 136.0),
                FabActionSlot(254.0, 136.0),
                FabActionSlot(318.0, 136.0),
            ),
            slots,
        )
    }
}
