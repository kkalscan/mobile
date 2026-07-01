package ru.kkalscan.app.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.components.MacroChipsRow
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.MealType
import kotlinx.coroutines.launch
import ru.kkalscan.presentation.food.IFoodSearchViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodSearchSheet(
    viewModel: IFoodSearchViewModel,
    onDismiss: () -> Unit,
    onAdded: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.addSuccess) {
        if (state.addSuccess) {
            viewModel.consumeAddSuccess()
            onAdded()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { if (!state.isAdding) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = KkalScanDimens.phoneMaxWidth - 32.dp)
                .fillMaxWidth()
                .testTag("food-search-sheet"),
            shape = RoundedCornerShape(24.dp),
            color = KkalScanColors.Background,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Найти продукт",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Поиск по каталогу — поможет понять, что ищут пользователи",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth().testTag("food-search-input"),
                    placeholder = { Text("Борщ, творог, овсянка…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    enabled = !state.isAdding,
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("Добавить как", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = state.selectedMealType == meal,
                            onClick = { viewModel.selectMealType(meal) },
                            label = { Text(meal.label()) },
                            enabled = !state.isAdding,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KkalScanColors.PrimaryContainer,
                                selectedLabelColor = KkalScanColors.Primary,
                            ),
                            shape = RoundedCornerShape(999.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                state.errorMessage?.let { message ->
                    KkalErrorBanner(message = message, onRetry = { viewModel.onQueryChange(state.query) })
                    Spacer(Modifier.height(12.dp))
                }

                when {
                    state.isSearching -> {
                        Row(
                            Modifier.fillMaxWidth().height(120.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(color = KkalScanColors.Primary)
                        }
                    }

                    state.query.trim().length < 2 -> {
                        Text(
                            "Введите минимум 2 символа",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KkalScanColors.OnSurfaceVariant,
                        )
                    }

                    state.results.isEmpty() -> {
                        Text(
                            "Ничего не найдено — запрос сохранён для аналитики",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KkalScanColors.OnSurfaceVariant,
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.results, key = { it.name }) { dish ->
                                FoodSearchResultRow(
                                    dish = dish,
                                    enabled = !state.isAdding,
                                    onClick = { scope.launch { viewModel.addDish(dish) } },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isAdding,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Закрыть", color = KkalScanColors.OnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FoodSearchResultRow(
    dish: Dish,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("food-search-result-${dish.name}"),
        shape = RoundedCornerShape(KkalScanDimens.cardRadius),
        color = KkalScanColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Outline.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(dish.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${dish.kcal} ккал · ${dish.grams} г",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KkalScanColors.Primary,
                )
                Spacer(Modifier.height(6.dp))
                MacroChipsRow(protein = dish.protein, fat = dish.fat, carbs = dish.carbs, fiber = dish.fiber)
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
