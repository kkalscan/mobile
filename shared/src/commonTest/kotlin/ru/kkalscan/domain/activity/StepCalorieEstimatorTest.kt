package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StepCalorieEstimatorTest {

    @Test
    fun estimate_19000steps_110kg() {
        val profile = EnergyProfile(weightKg = 110.0)
        StepCalorieEstimator.estimate(19_000, profile) shouldBe 1143
    }

    @Test
    fun estimate_zeroSteps_returnsZero() {
        StepCalorieEstimator.estimate(0) shouldBe 0
    }
}
