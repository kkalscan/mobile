package ru.kkalscan.domain.activity

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BmrCalculatorTest {

    @Test
    fun male110kg_180cm_40y() {
        val profile = EnergyProfile(weightKg = 110.0, heightCm = 180.0, ageYears = 40)
        BmrCalculator.dailyBmr(profile) shouldBe 2030
    }

    @Test
    fun proratedHalfDay() {
        val profile = EnergyProfile(weightKg = 80.0, heightCm = 180.0, ageYears = 30)
        val half = BmrCalculator.proratedBmr(profile, 0.5)
        half shouldBe BmrCalculator.dailyBmr(profile) / 2
    }
}
