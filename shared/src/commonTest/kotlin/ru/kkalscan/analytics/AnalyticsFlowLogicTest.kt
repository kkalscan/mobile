package ru.kkalscan.analytics

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class AnalyticsFlowLogicTest {

    @Test
    fun photoScanHappyPath_firstScan() {
        AnalyticsFlowLogic.photoScanHappyPath(scansLeftAfterSuccess = 3) shouldContainExactly listOf(
            AnalyticsEvents.SCAN_OPEN,
            AnalyticsEvents.PHOTO_SELECTED,
            AnalyticsEvents.PHOTO_SCAN,
            AnalyticsEvents.SCAN_SUCCESS,
            AnalyticsEvents.FIRST_SCAN_SUCCESS,
            AnalyticsEvents.ADD_TO_DIARY,
        )
    }

    @Test
    fun photoScanHappyPath_thirdScan() {
        AnalyticsFlowLogic.photoScanHappyPath(scansLeftAfterSuccess = 1) shouldContainExactly listOf(
            AnalyticsEvents.SCAN_OPEN,
            AnalyticsEvents.PHOTO_SELECTED,
            AnalyticsEvents.PHOTO_SCAN,
            AnalyticsEvents.SCAN_SUCCESS,
            AnalyticsEvents.THIRD_SCAN_SUCCESS,
            AnalyticsEvents.ADD_TO_DIARY,
        )
    }

    @Test
    fun describeFoodHappyPath_matchesPhotoFunnelExceptEntry() {
        val describe = AnalyticsFlowLogic.describeFoodHappyPath(scansLeftAfterSuccess = 2)
        val photo = AnalyticsFlowLogic.photoScanHappyPath(scansLeftAfterSuccess = 2)

        describe.first() shouldBe AnalyticsEvents.DESCRIBE_FOOD_OPEN
        describe[1] shouldBe AnalyticsEvents.DESCRIBE_TEXT_SCAN
        describe.filter {
            it in setOf(AnalyticsEvents.SCAN_SUCCESS, AnalyticsEvents.SECOND_SCAN_SUCCESS, AnalyticsEvents.ADD_TO_DIARY)
        } shouldContainExactly listOf(
            AnalyticsEvents.SCAN_SUCCESS,
            AnalyticsEvents.SECOND_SCAN_SUCCESS,
            AnalyticsEvents.ADD_TO_DIARY,
        )
        describe.last() shouldBe AnalyticsEvents.ADD_TO_DIARY
        describe.contains(AnalyticsEvents.DESCRIBE_FOOD_RECOGNIZED) shouldBe true
        describe.contains(AnalyticsEvents.DESCRIBE_TEXT_SCAN) shouldBe true

        photo.contains(AnalyticsEvents.PHOTO_SELECTED) shouldBe true
        photo.contains(AnalyticsEvents.PHOTO_SCAN) shouldBe true
        describe.contains(AnalyticsEvents.PHOTO_SELECTED) shouldBe false
        describe.contains(AnalyticsEvents.PHOTO_SCAN) shouldBe false
    }

    @Test
    fun photoScanLimitHitPath() {
        AnalyticsFlowLogic.photoScanLimitHitPath() shouldContainExactly listOf(
            AnalyticsEvents.SCAN_OPEN,
            AnalyticsEvents.PHOTO_SELECTED,
            AnalyticsEvents.PHOTO_SCAN,
            AnalyticsEvents.LIMIT_HIT,
            AnalyticsEvents.PAYWALL_SHOWN,
            AnalyticsEvents.AD_OFFER_SHOWN,
        )
    }

    @Test
    fun describeLimitHitPath() {
        AnalyticsFlowLogic.describeLimitHitPath() shouldContainExactly listOf(
            AnalyticsEvents.DESCRIBE_FOOD_OPEN,
            AnalyticsEvents.DESCRIBE_TEXT_SCAN,
            AnalyticsEvents.LIMIT_HIT,
            AnalyticsEvents.PAYWALL_SHOWN,
            AnalyticsEvents.AD_OFFER_SHOWN,
        )
    }

    @Test
    fun rewardedAdSuccessPath() {
        val event = AnalyticsFlowLogic.adWatchCompleteEvent(scansLeft = 4)
        event.name shouldBe AnalyticsEvents.AD_WATCH_COMPLETE
        event.attributes["scans_left"] shouldBe "4"

        AnalyticsFlowLogic.rewardedAdSuccessPath().take(2) shouldContainExactly listOf(
            AnalyticsEvents.AD_BONUS_CLICK,
            AnalyticsEvents.AD_WATCH_COMPLETE,
        )
    }

    @Test
    fun rewardedAdFailedPath() {
        AnalyticsFlowLogic.rewardedAdFailedPath("network").map { it.name } shouldContainExactly listOf(
            AnalyticsEvents.AD_BONUS_CLICK,
            AnalyticsEvents.AD_BONUS_FAILED,
        )
    }

    @Test
    fun proPurchasePath() {
        AnalyticsFlowLogic.proPurchasePath() shouldContainExactly listOf(
            AnalyticsEvents.PRO_CLICK,
            AnalyticsEvents.SUBSCRIPTION_START,
        )
    }

    @Test
    fun addToDiaryFailedPath() {
        AnalyticsFlowLogic.addToDiaryFailedPath("api").map { it.name } shouldContainExactly listOf(
            AnalyticsEvents.ADD_TO_DIARY,
            AnalyticsEvents.ADD_TO_DIARY_FAILED,
        )
    }

    @Test
    fun featureSearchQueryEvent_attributes() {
        val event = AnalyticsFlowLogic.featureSearchQueryEvent("профиль", resultsCount = 2)
        event.name shouldBe AnalyticsEvents.FEATURE_SEARCH_QUERY
        event.attributes["query"] shouldBe "профиль"
        event.attributes["results"] shouldBe "2"
        event.attributes["empty_query"] shouldBe "false"
    }

    @Test
    fun scanFunnel_isSubsetOfPhotoHappyPath() {
        val happy = AnalyticsFlowLogic.photoScanHappyPath(3)
        AnalyticsEvents.scanFunnel.forEach { step ->
            assert(happy.contains(step)) { "Scan funnel step $step missing from photo happy path" }
        }
    }

    @Test
    fun describeFunnel_isSubsetOfDescribeHappyPath() {
        val happy = AnalyticsFlowLogic.describeFoodHappyPath(3)
        AnalyticsEvents.describeFunnel.forEach { step ->
            assert(happy.contains(step)) { "Describe funnel step $step missing from describe happy path" }
        }
    }

    @Test
    fun monetizationFunnel_stepsAreImplemented() {
        val limitPath = AnalyticsFlowLogic.photoScanLimitHitPath()
        val adPath = AnalyticsFlowLogic.rewardedAdSuccessPath()
        val proPath = AnalyticsFlowLogic.proPurchasePath()

        limitPath.contains(AnalyticsEvents.THIRD_SCAN_SUCCESS) shouldBe false
        limitPath.contains(AnalyticsEvents.LIMIT_HIT) shouldBe true
        adPath.contains(AnalyticsEvents.AD_BONUS_CLICK) shouldBe true
        adPath.contains(AnalyticsEvents.AD_WATCH_COMPLETE) shouldBe true
        proPath.contains(AnalyticsEvents.SUBSCRIPTION_START) shouldBe true

        AnalyticsEvents.monetizationFunnel.forEach { step ->
            assert(step in AnalyticsEvents.allImplemented) { "Monetization step $step not in catalog" }
        }
    }
}
