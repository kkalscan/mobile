package ru.kkalscan.analytics

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ScanAnalyticsLogicTest {

    @Test
    fun scanSuccess_emitsMilestone() {
        ScanAnalyticsLogic.scanSuccessEvents(2).map { it.name } shouldBe listOf(
            AnalyticsEvents.SCAN_SUCCESS,
            AnalyticsEvents.SECOND_SCAN_SUCCESS,
        )
    }

    @Test
    fun scanOutcome_limitHit() {
        ScanAnalyticsLogic.scanOutcomeEvents(
            scansLeft = 0,
            limitHit = true,
            hasResult = false,
            errorMessage = null,
        ).single().name shouldBe AnalyticsEvents.LIMIT_HIT
    }

    @Test
    fun scanOutcome_error() {
        ScanAnalyticsLogic.scanOutcomeEvents(
            scansLeft = 2,
            limitHit = false,
            hasResult = false,
            errorMessage = "network",
        ).single().name shouldBe AnalyticsEvents.SCAN_ERROR
    }
}
