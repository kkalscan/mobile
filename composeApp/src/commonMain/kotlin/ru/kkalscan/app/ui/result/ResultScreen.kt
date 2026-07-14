package ru.kkalscan.app.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.components.KkalFoodCard
import ru.kkalscan.app.components.toMealIconKind
import ru.kkalscan.app.components.KkalHeroCard
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.presentation.scan.IScanViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    viewModel: IScanViewModel,
    onAddToDiary: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val result = state.result ?: run {
        onBack()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KkalScanDimens.screenHorizontal),
    ) {
        Spacer(Modifier.height(16.dp))
        KkalPageHeader(title = "Результат скана")
        Spacer(Modifier.height(16.dp))
        KkalHeroCard(
            title = "ИТОГО",
            kcal = result.totalKcal,
            subtitle = result.dishes.joinToString(", ") { it.name }.take(80),
            badge = null,
            protein = result.totalProtein,
            fat = result.totalFat,
            carbs = result.totalCarbs,
            fiber = result.totalFiber,
            watermark = "OK",
        )
        Spacer(Modifier.height(8.dp))
        Text("Блюда", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        result.dishes.forEach { dish ->
            KkalFoodCard(
                title = dish.name,
                kcal = dish.kcal,
                subtitle = "${dish.grams} г",
                macros = Triple(dish.protein, dish.fat, dish.carbs),
                fiber = dish.fiber,
                mealIcon = state.selectedMealType.toMealIconKind(),
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Добавить как ${state.selectedMealType.label().lowercase()}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealType.entries.forEach { meal ->
                FilterChip(
                    selected = state.selectedMealType == meal,
                    onClick = { viewModel.selectMealType(meal) },
                    label = { Text(meal.label()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KkalScanColors.PrimaryContainer,
                        selectedLabelColor = KkalScanColors.Primary,
                    ),
                    shape = RoundedCornerShape(999.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        KkalPrimaryButton(
            text = "Добавить в дневник",
            onClick = onAddToDiary,
            loading = state.isSaving,
            containerColor = KkalScanColors.Secondary,
        )
        Spacer(Modifier.height(12.dp))
        KkalPrimaryButton(
            text = "Сканировать ещё",
            onClick = onBack,
            containerColor = KkalScanColors.SurfaceVariant,
            contentColor = KkalScanColors.OnBackground,
        )
        Spacer(Modifier.height(100.dp))
    }
}

private fun MealType.label(): String = when (this) {
    MealType.breakfast -> "Завтрак"
    MealType.lunch -> "Обед"
    MealType.dinner -> "Ужин"
    MealType.snack -> "Перекус"
}
