package ru.kkalscan.onboarding

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FabAttentionSchedulerTest {

    private val scheduler = FabAttentionScheduler(intervalMs = 60_000)

    @Test
    fun firstShow_whenNeverLogged() {
        scheduler.shouldShow(
            nowMs = 1_000,
            hasLoggedAnything = false,
            fabExpanded = false,
            loading = false,
            lastShownMs = null,
        ) shouldBe true
    }

    @Test
    fun skip_whenLogged() {
        scheduler.shouldShow(
            nowMs = 1_000,
            hasLoggedAnything = true,
            fabExpanded = false,
            loading = false,
            lastShownMs = null,
        ) shouldBe false
    }

    @Test
    fun skip_whenExpandedOrLoading() {
        scheduler.shouldShow(
            nowMs = 1_000,
            hasLoggedAnything = false,
            fabExpanded = true,
            loading = false,
            lastShownMs = null,
        ) shouldBe false
        scheduler.shouldShow(
            nowMs = 1_000,
            hasLoggedAnything = false,
            fabExpanded = false,
            loading = true,
            lastShownMs = null,
        ) shouldBe false
    }

    @Test
    fun respectInterval() {
        scheduler.shouldShow(
            nowMs = 50_000,
            hasLoggedAnything = false,
            fabExpanded = false,
            loading = false,
            lastShownMs = 0,
        ) shouldBe false
        scheduler.shouldShow(
            nowMs = 60_000,
            hasLoggedAnything = false,
            fabExpanded = false,
            loading = false,
            lastShownMs = 0,
        ) shouldBe true
    }
}
