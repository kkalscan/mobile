package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StepCalorieEstimatorTest {

    @Test
    fun estimate_56steps_returns2kcal() {
        StepCalorieEstimator.estimate(56) shouldBe 2
    }

    @Test
    fun estimate_zeroSteps_returnsZero() {
        StepCalorieEstimator.estimate(0) shouldBe 0
    }
}
