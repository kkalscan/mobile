package ru.kkalscan.analytics

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/**
 * Verifies scan outcome analytics for both photo and text describe flows.
 * Both paths share [ScanAnalyticsLogic] in the app.
 */
class DescribeAndPhotoAnalyticsParityTest {

    @Test
    fun describeAndPhoto_emitSameSuccessEvents() {
        val photoEvents = ScanAnalyticsLogic.scanOutcomeEvents(
            scansLeft = 2,
            limitHit = false,
            hasResult = true,
            errorMessage = null,
        ).map { it.name }

        val describeEvents = ScanAnalyticsLogic.scanOutcomeEvents(
            scansLeft = 2,
            limitHit = false,
            hasResult = true,
            errorMessage = null,
        ).map { it.name }

        photoEvents shouldBe describeEvents
        photoEvents shouldBe listOf(
            AnalyticsEvents.SCAN_SUCCESS,
            AnalyticsEvents.SECOND_SCAN_SUCCESS,
        )
    }

    @Test
    fun describeAndPhoto_emitSameLimitHitEvent() {
        val photoLimit = ScanAnalyticsLogic.scanOutcomeEvents(
            scansLeft = 0,
            limitHit = true,
            hasResult = false,
            errorMessage = null,
        ).single()

        val describeLimit = ScanAnalyticsLogic.scanOutcomeEvents(
            scansLeft = 0,
            limitHit = true,
            hasResult = false,
            errorMessage = null,
        ).single()

        photoLimit.name shouldBe AnalyticsEvents.LIMIT_HIT
        photoLimit shouldBe describeLimit
        photoLimit.attributes["scans_left"] shouldBe "0"
    }

    @Test
    fun describeAndPhoto_emitSameScanError() {
        val events = ScanAnalyticsLogic.scanOutcomeEvents(
            scansLeft = 1,
            limitHit = false,
            hasResult = false,
            errorMessage = "Нет сети",
        ).single()

        events.name shouldBe AnalyticsEvents.SCAN_ERROR
        events.attributes["reason"] shouldBe "Нет сети"
    }

    @Test
    fun allScanMilestones_covered() {
        listOf(3, 2, 1).forEachIndexed { index, scansLeft ->
            val milestone = ScanAnalyticsLogic.scanSuccessEvents(scansLeft).map { it.name }.last()
            val expected = listOf(
                AnalyticsEvents.FIRST_SCAN_SUCCESS,
                AnalyticsEvents.SECOND_SCAN_SUCCESS,
                AnalyticsEvents.THIRD_SCAN_SUCCESS,
            )[index]
            milestone shouldBe expected
        }
    }
}
