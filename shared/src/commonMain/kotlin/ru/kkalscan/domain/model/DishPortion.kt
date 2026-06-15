package ru.kkalscan.domain.model

import kotlin.math.roundToInt

object DishPortion {
    const val MIN_GRAMS = 10
    const val MAX_GRAMS = 3_000
    const val STEP_GRAMS = 10

    fun withGrams(dish: Dish, newGrams: Int): Dish {
        val grams = newGrams.coerceIn(MIN_GRAMS, MAX_GRAMS)
        if (dish.grams <= 0) return dish.copy(grams = grams)
        val ratio = grams.toDouble() / dish.grams
        return dish.copy(
            grams = grams,
            kcal = (dish.kcal * ratio).roundToInt().coerceAtLeast(0),
            protein = roundMacro(dish.protein * ratio),
            fat = roundMacro(dish.fat * ratio),
            carbs = roundMacro(dish.carbs * ratio),
        )
    }

    fun scaledFromBaseline(dish: Dish, baseline: Dish, factor: Double): Dish {
        val targetGrams = (baseline.grams * factor).roundToInt().coerceIn(MIN_GRAMS, MAX_GRAMS)
        if (baseline.grams <= 0) return withGrams(dish, targetGrams)
        val ratio = targetGrams.toDouble() / baseline.grams
        return dish.copy(
            grams = targetGrams,
            kcal = (baseline.kcal * ratio).roundToInt().coerceAtLeast(0),
            protein = roundMacro(baseline.protein * ratio),
            fat = roundMacro(baseline.fat * ratio),
            carbs = roundMacro(baseline.carbs * ratio),
        )
    }

    fun totals(dishes: List<Dish>): MacroTotals = MacroTotals(
        kcal = dishes.sumOf { it.kcal },
        protein = roundMacro(dishes.sumOf { it.protein }),
        fat = roundMacro(dishes.sumOf { it.fat }),
        carbs = roundMacro(dishes.sumOf { it.carbs }),
    )

    private fun roundMacro(value: Double): Double = (value * 10).roundToInt() / 10.0
}

data class MacroTotals(
    val kcal: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)
