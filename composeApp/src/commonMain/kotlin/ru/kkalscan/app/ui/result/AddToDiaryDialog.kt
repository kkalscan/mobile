package ru.kkalscan.app.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.kkalscan.app.components.KkalFoodCard
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.presentation.scan.IScanViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddToDiaryDialog(
    viewModel: IScanViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val result = state.result ?: return

    Dialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = KkalScanDimens.phoneMaxWidth - 32.dp)
                .fillMaxWidth()
                .testTag("scan-result-dialog"),
            shape = RoundedCornerShape(24.dp),
            color = KkalScanColors.Background,
            shadowElevation = 16.dp,
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Распознано",
                    style = MaterialTheme.typography.labelLarge,
                    color = KkalScanColors.Primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${result.totalKcal} ккал · ${result.dishes.size} блюд",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(16.dp))
                Text("Блюда", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                result.dishes.forEach { dish ->
                    KkalFoodCard(
                        title = dish.name,
                        kcal = dish.kcal,
                        subtitle = "${dish.grams} г",
                        macros = Triple(dish.protein, dish.fat, dish.carbs),
                        iconLabel = dish.name.firstOrNull()?.uppercaseChar()?.toString() ?: "K",
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    "Добавить как ${state.selectedMealType.label().lowercase()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = state.selectedMealType == meal,
                            onClick = { viewModel.selectMealType(meal) },
                            label = { Text(meal.label()) },
                            enabled = !state.isSaving,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KkalScanColors.PrimaryContainer,
                                selectedLabelColor = KkalScanColors.Primary,
                            ),
                            shape = RoundedCornerShape(999.dp),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                KkalPrimaryButton(
                    text = "Добавить в дневник",
                    onClick = onConfirm,
                    loading = state.isSaving,
                    containerColor = KkalScanColors.Secondary,
                )
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Не добавлять",
                        color = KkalScanColors.OnSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

private fun MealType.label(): String = when (this) {
    MealType.breakfast -> "Завтрак"
    MealType.lunch -> "Обед"
    MealType.dinner -> "Ужин"
    MealType.snack -> "Перекус"
}
