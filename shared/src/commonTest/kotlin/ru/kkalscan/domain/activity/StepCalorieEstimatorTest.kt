package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StepCalorieEstimatorTest {

    @Test
    fun estimate_56steps_returns3kcal() {
        StepCalorieEstimator.estimate(56) shouldBe 3
    }

    @Test
    fun estimate_19000steps_returns1045kcal() {
        StepCalorieEstimator.estimate(19_000) shouldBe 1045
    }

    @Test
    fun estimate_zeroSteps_returnsZero() {
        StepCalorieEstimator.estimate(0) shouldBe 0
    }
}
