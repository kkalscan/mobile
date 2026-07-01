package ru.kkalscan.app.ui.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.kkalscan.app.components.FoodPhotoThumbnail
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.MacroChipsRow
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.Dish
import ru.kkalscan.domain.model.DishPortion
import ru.kkalscan.domain.model.MealType
import ru.kkalscan.app.platform.updateMaestroDebugState
import ru.kkalscan.presentation.scan.IScanViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddToDiaryDialog(
    viewModel: IScanViewModel,
    onDismiss: () -> Unit,
    onConfirm: suspend () -> Boolean,
    onRefreshAfterAdd: () -> Unit,
    onAddFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val result = state.result ?: return
    val baseline = state.baselineDishes
    val dish = result.dishes.firstOrNull()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onRefreshAfterAdd()
            delay(SUCCESS_DISMISS_MS)
            onAddFinished()
        }
    }

    SideEffect {
        updateMaestroDebugState(
            buildString {
                append("scan-result kcal=${result.totalKcal}")
                append(" grams=${dish?.grams ?: 0}")
                append(" p=${dish?.protein ?: 0}")
                append(" f=${dish?.fat ?: 0}")
                append(" c=${dish?.carbs ?: 0}")
            },
        )
    }

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
            Box {
                Column(
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                Text(
                    "Проверьте порции",
                    style = MaterialTheme.typography.labelLarge,
                    color = KkalScanColors.Primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${result.totalKcal} ккал · ${result.dishes.size} блюд",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "AI оценил порцию — поправьте граммы, если знаете точнее",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                result.dishes.forEachIndexed { index, dish ->
                    val aiGrams = baseline.getOrNull(index)?.grams ?: dish.grams
                    EditableDishCard(
                        dish = dish,
                        photoBytes = state.photoBytes,
                        aiGrams = aiGrams,
                        canRemove = result.dishes.size > 1,
                        enabled = !state.isSaving,
                        onDecrease = { viewModel.adjustDishGrams(index, -DishPortion.STEP_GRAMS) },
                        onIncrease = { viewModel.adjustDishGrams(index, DishPortion.STEP_GRAMS) },
                        onScale = { factor -> viewModel.scaleDishFromBaseline(index, factor) },
                        onRemove = { viewModel.removeDish(index) },
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
                    onClick = {
                        scope.launch {
                            if (!onConfirm()) return@launch
                        }
                    },
                    loading = state.isSaving && !state.saveSuccess,
                    enabled = result.dishes.isNotEmpty() && !state.isSaving,
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
                if (state.isSaving) {
                    AddToDiaryProgressOverlay(
                        success = state.saveSuccess,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private const val SUCCESS_DISMISS_MS = 950L

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableDishCard(
    dish: Dish,
    photoBytes: ByteArray?,
    aiGrams: Int,
    canRemove: Boolean,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onScale: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KkalScanDimens.cardRadius),
        color = KkalScanColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Outline.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                FoodPhotoThumbnail(
                    photoBytes = photoBytes,
                    fallbackLabel = dish.name.firstOrNull()?.uppercaseChar()?.toString() ?: "K",
                    modifier = Modifier.size(KkalScanDimens.thumbSize),
                    contentDescription = dish.name,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        dish.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${dish.kcal} ккал",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KkalScanColors.Primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    MacroChipsRow(protein = dish.protein, fat = dish.fat, carbs = dish.carbs, fiber = dish.fiber)
                }
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        enabled = enabled,
                        modifier = Modifier.testTag("dish-remove"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = KkalScanColors.OnSurfaceVariant),
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Удалить блюдо")
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = onDecrease,
                    enabled = enabled && dish.grams > DishPortion.MIN_GRAMS,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dish-grams-minus"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = KkalScanColors.PrimaryContainer,
                        contentColor = KkalScanColors.Primary,
                    ),
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Меньше на ${DishPortion.STEP_GRAMS} г")
                }
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "${dish.grams} г",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("dish-grams-value"),
                    )
                    Text(
                        "порция",
                        style = MaterialTheme.typography.labelMedium,
                        color = KkalScanColors.OnSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onIncrease,
                    enabled = enabled && dish.grams < DishPortion.MAX_GRAMS,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dish-grams-plus"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = KkalScanColors.PrimaryContainer,
                        contentColor = KkalScanColors.Primary,
                    ),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Больше на ${DishPortion.STEP_GRAMS} г")
                }
            }
            if (aiGrams > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Быстрая порция от AI ($aiGrams г)",
                    style = MaterialTheme.typography.labelLarge,
                    color = KkalScanColors.OnSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PortionChip("½", enabled, Modifier.weight(1f)) { onScale(0.5) }
                    PortionChip("1×", enabled, Modifier.weight(1f)) { onScale(1.0) }
                    PortionChip("2×", enabled, Modifier.weight(1f)) { onScale(2.0) }
                }
            }
        }
    }
}

@Composable
private fun PortionChip(label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = KkalScanColors.Background,
            labelColor = KkalScanColors.OnBackground,
        ),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier.testTag("dish-portion-$label"),
    )
}

private fun MealType.label(): String = when (this) {
    MealType.breakfast -> "Завтрак"
    MealType.lunch -> "Обед"
    MealType.dinner -> "Ужин"
    MealType.snack -> "Перекус"
}
