package ru.kkalscan.domain.model

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DishPortionTest {

    private val borscht = Dish(
        name = "Борщ",
        grams = 300,
        kcal = 250,
        protein = 12.0,
        fat = 8.0,
        carbs = 22.0,
        fiber = 6.0,
    )

    @Test
    fun withGrams_scalesMacrosProportionally() {
        val half = DishPortion.withGrams(borscht, 150)
        half.grams shouldBe 150
        half.kcal shouldBe 125
        half.protein shouldBe 6.0
        half.fat shouldBe 4.0
        half.carbs shouldBe 11.0
        half.fiber shouldBe 3.0
    }

    @Test
    fun withGrams_clampsToMinAndMax() {
        DishPortion.withGrams(borscht, 1).grams shouldBe DishPortion.MIN_GRAMS
        DishPortion.withGrams(borscht, 9_999).grams shouldBe DishPortion.MAX_GRAMS
    }

    @Test
    fun scaledFromBaseline_usesAiPortionAsReference() {
        val doubled = DishPortion.scaledFromBaseline(borscht, borscht, 2.0)
        doubled.grams shouldBe 600
        doubled.kcal shouldBe 500
    }

    @Test
    fun scaledFromBaseline_halfPortion() {
        val half = DishPortion.scaledFromBaseline(borscht, borscht, 0.5)
        half.grams shouldBe 150
        half.kcal shouldBe 125
    }

    @Test
    fun totals_sumsAllDishes() {
        val totals = DishPortion.totals(
            listOf(
                borscht,
                borscht.copy(name = "Хлеб", grams = 50, kcal = 120, protein = 4.0, fat = 1.0, carbs = 20.0, fiber = 2.5),
            ),
        )
        totals.kcal shouldBe 370
        totals.protein shouldBe 16.0
        totals.fiber shouldBe 8.5
    }
}
