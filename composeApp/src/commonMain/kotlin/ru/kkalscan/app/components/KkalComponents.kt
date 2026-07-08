package ru.kkalscan.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.platform.isStoreScreenshotMode
import ru.kkalscan.app.platform.MaestroFabController
import ru.kkalscan.app.platform.updateMaestroFabState
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.DiaryEntry

@Composable
fun KkalScreenScaffold(
    hasBottomBar: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isStoreScreenshotMode()) KkalScanColors.Background else KkalScanColors.PageOuter,
            ),
    ) {
        val phoneWidth = when {
            maxWidth < KkalScanDimens.phoneMaxWidth -> maxWidth
            else -> KkalScanDimens.phoneMaxWidth
        }
        Box(
            modifier = Modifier
                .width(phoneWidth)
                .fillMaxHeight()
                .align(Alignment.TopCenter)
                .then(if (isStoreScreenshotMode()) Modifier else Modifier.shadow(8.dp))
                .background(KkalScanColors.Background),
        ) {
            KkalPageGradient()
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                topBar?.let { bar ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = KkalScanDimens.screenHorizontal,
                                vertical = 12.dp,
                            ),
                    ) {
                        bar()
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(if (!hasBottomBar) Modifier.navigationBarsPadding() else Modifier),
                ) {
                    content()
                }
                if (hasBottomBar) {
                    Box(Modifier.navigationBarsPadding()) {
                        bottomBar()
                    }
                }
            }
        }
    }
}

@Composable
private fun KkalPageGradient() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(
                Brush.verticalGradient(
                    listOf(KkalScanColors.HeaderGradientTop, KkalScanColors.HeaderGradientBottom),
                ),
            ),
    )
}

@Composable
fun KkalPageHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.headlineLarge)
}

@Composable
fun KkalBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onDescribeClick: () -> Unit,
    onAddWorkoutClick: () -> Unit,
    onScanClick: () -> Unit,
    actionLoading: Boolean = false,
) {
    Box(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = KkalScanColors.Surface,
            shadowElevation = 16.dp,
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BottomTab(
                    label = "Сегодня",
                    icon = KkalNavIconType.Today,
                    selected = selectedTab == AppTab.Today,
                    onClick = { onTabSelected(AppTab.Today) },
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BottomTab(
                    label = "Дневник",
                    icon = KkalNavIconType.Journal,
                    selected = selectedTab == AppTab.Journal,
                    onClick = { onTabSelected(AppTab.Journal) },
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BottomTab(
                    label = "Профиль",
                    icon = KkalNavIconType.Profile,
                    selected = selectedTab == AppTab.Profile,
                    onClick = { onTabSelected(AppTab.Profile) },
                )
            }
        }
        }
        if (selectedTab == AppTab.Today) {
            var isFabExpanded by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(isFabExpanded) {
                updateMaestroFabState(isFabExpanded)
            }

            DisposableEffect(actionLoading) {
                MaestroFabController.onTapMainFab = {
                    if (!actionLoading) {
                        isFabExpanded = !isFabExpanded
                    }
                }
                onDispose {
                    MaestroFabController.onTapMainFab = null
                    updateMaestroFabState(false)
                }
            }

            AnimatedVisibility(
                visible = isFabExpanded,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = KkalScanDimens.fabEndPadding)
                    .offset(y = -(KkalScanDimens.fabFloatOffset + 74.dp)),
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DiaryFabAction(
                        testTag = "diary-fab-describe-food",
                        contentDescription = "Описать еду",
                        icon = Icons.Outlined.Edit,
                        onClick = {
                            if (!actionLoading) {
                                isFabExpanded = false
                                onDescribeClick()
                            }
                        },
                    )
                    DiaryFabAction(
                        testTag = "diary-fab-add-workout",
                        contentDescription = "Добавить тренировку",
                        icon = Icons.Outlined.FitnessCenter,
                        onClick = {
                            if (!actionLoading) {
                                isFabExpanded = false
                                onAddWorkoutClick()
                            }
                        },
                    )
                    DiaryFabAction(
                        testTag = "diary-fab-scan-photo",
                        contentDescription = "Скан фото",
                        icon = Icons.Outlined.CameraAlt,
                        onClick = {
                            if (!actionLoading) {
                                isFabExpanded = false
                                onScanClick()
                            }
                        },
                    )
                }
            }

            Surface(
                onClick = {
                    if (!actionLoading) {
                        isFabExpanded = !isFabExpanded
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = KkalScanDimens.fabEndPadding)
                    .offset(y = -KkalScanDimens.fabFloatOffset)
                    .size(KkalScanDimens.fabSize)
                    .testTag("diary-main-fab")
                    .shadow(12.dp, CircleShape, ambientColor = KkalScanColors.Primary.copy(0.35f)),
                shape = CircleShape,
                color = KkalScanColors.Primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (actionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = KkalScanColors.OnPrimary,
                            strokeWidth = 3.dp,
                        )
                    } else if (isFabExpanded) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Закрыть",
                            tint = KkalScanColors.OnPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Добавить",
                            tint = KkalScanColors.OnPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryFabAction(
    testTag: String,
    contentDescription: String,
    label: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .testTag(testTag)
            .shadow(10.dp, CircleShape, ambientColor = KkalScanColors.Primary.copy(0.25f)),
        shape = CircleShape,
        color = KkalScanColors.Primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = KkalScanColors.OnPrimary,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Text(
                    text = label ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = KkalScanColors.OnPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

enum class AppTab { Today, Journal, Profile }

@Composable
private fun BottomTab(
    label: String,
    icon: KkalNavIconType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KkalNavIcon(type = icon, selected = selected)
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) KkalScanColors.Primary else KkalScanColors.OnSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun KkalHeroCard(
    title: String,
    kcal: Int,
    subtitle: String?,
    badge: String?,
    protein: Double? = null,
    fat: Double? = null,
    carbs: Double? = null,
    fiber: Double? = null,
    watermark: String? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(KkalScanDimens.cardRadius), ambientColor = Color(0x14000000)),
        shape = RoundedCornerShape(KkalScanDimens.cardRadius),
        color = KkalScanColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Outline.copy(alpha = 0.6f)),
    ) {
        Box {
            watermark?.let {
                Text(
                    it,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp),
                    style = MaterialTheme.typography.displayMedium,
                    color = KkalScanColors.Outline.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.padding(24.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = KkalScanColors.OnSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$kcal",
                        style = MaterialTheme.typography.displayLarge,
                        color = KkalScanColors.Primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "ккал",
                        style = MaterialTheme.typography.titleMedium,
                        color = KkalScanColors.OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (protein != null || fat != null || carbs != null || fiber != null) {
                    Spacer(Modifier.height(16.dp))
                    MacroChipsRow(protein = protein, fat = fat, carbs = carbs, fiber = fiber)
                }
                subtitle?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = KkalScanColors.OnSurfaceVariant)
                }
                badge?.let {
                    Spacer(Modifier.height(16.dp))
                    ScanBadge(text = it)
                }
            }
        }
    }
}

@Composable
fun ScanBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(min = 72.dp),
        shape = RoundedCornerShape(999.dp),
        color = KkalScanColors.TertiaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(KkalScanColors.Primary),
            )
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = KkalScanColors.OnBackground,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacroChipsRow(
    protein: Double?,
    fat: Double?,
    carbs: Double?,
    fiber: Double? = null,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        protein?.let { MacroChip("Б", it, KkalScanColors.Protein, Color(0xFFE8F0FF)) }
        fat?.let { MacroChip("Ж", it, KkalScanColors.Fat, Color(0xFFFFF0E0)) }
        carbs?.let { MacroChip("У", it, KkalScanColors.Carbs, KkalScanColors.CarbsContainer) }
        fiber?.let { MacroChip("Кл", it, KkalScanColors.Fiber, Color(0xFFE1F7EF)) }
    }
}

@Composable
fun MacroChip(label: String, grams: Double, color: Color, bg: Color) {
    Surface(shape = RoundedCornerShape(KkalScanDimens.chipRadius), color = bg) {
        Text(
            "$label ${grams.toInt()}г",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun KkalTipCard(
    number: String,
    title: String,
    body: String,
    onClick: (() -> Unit)? = null,
    badgeIcon: ImageVector? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .shadow(8.dp, RoundedCornerShape(KkalScanDimens.cardRadius)),
        shape = RoundedCornerShape(KkalScanDimens.cardRadius),
        color = KkalScanColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Outline.copy(alpha = 0.5f)),
    ) {
        Box(Modifier.clip(RoundedCornerShape(KkalScanDimens.cardRadius))) {
            if (badgeIcon != null) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 16.dp)
                        .size(40.dp),
                    tint = KkalScanColors.Outline.copy(alpha = 0.55f),
                )
            } else {
                Text(
                    number,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 20.dp),
                    style = if (number.length <= 2) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    color = KkalScanColors.Outline.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = KkalScanColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable
fun KkalFoodCard(
    title: String,
    kcal: Int,
    subtitle: String? = null,
    tipBadge: String? = null,
    macros: Triple<Double, Double, Double>? = null,
    fiber: Double? = null,
    iconLabel: String = "K",
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(KkalScanDimens.cardRadius)),
        shape = RoundedCornerShape(KkalScanDimens.cardRadius),
        color = KkalScanColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Outline.copy(alpha = 0.4f)),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KkalIconBadge(label = iconLabel, modifier = Modifier.size(KkalScanDimens.thumbSize))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$kcal ккал",
                    style = MaterialTheme.typography.headlineMedium,
                    color = KkalScanColors.Primary,
                )
                tipBadge?.let {
                    Spacer(Modifier.height(8.dp))
                    ScanBadge(text = it)
                } ?: subtitle?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = KkalScanColors.OnSurfaceVariant)
                }
                if (macros != null || fiber != null) {
                    Spacer(Modifier.height(8.dp))
                    val (p, f, c) = macros ?: Triple(null, null, null)
                    MacroChipsRow(protein = p, fat = f, carbs = c, fiber = fiber)
                }
            }
        }
    }
}


@Composable
fun KkalCalorieBalanceCard(eatenKcal: Int, burnedKcal: Int, deficitKcal: Int, healthConnectKcal: Int, workoutKcal: Int, steps: Int? = null, modifier: Modifier = Modifier) {
    val deficitColor = when { deficitKcal > 0 -> KkalScanColors.Secondary; deficitKcal < 0 -> KkalScanColors.Error; else -> KkalScanColors.OnSurfaceVariant }
    val deficitLabel = when { deficitKcal > 0 -> "Дефицит"; deficitKcal < 0 -> "Профицит"; else -> "Баланс" }
    Surface(modifier.fillMaxWidth().testTag("calorie-balance-card").shadow(16.dp, RoundedCornerShape(KkalScanDimens.cardRadius), ambientColor = Color(0x14000000)), shape = RoundedCornerShape(KkalScanDimens.cardRadius), color = KkalScanColors.Surface, border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Outline.copy(alpha = 0.6f))) {
        Column(Modifier.padding(24.dp)) {
            Text("БАЛАНС КАЛОРИЙ", style = MaterialTheme.typography.labelSmall, color = KkalScanColors.OnSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BalanceMetric("Поступление", eatenKcal, KkalScanColors.Primary)
                BalanceMetric("Расход", burnedKcal, KkalScanColors.Protein)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (deficitKcal > 0) "+$deficitKcal" else deficitKcal.toString(), style = MaterialTheme.typography.displayLarge, color = deficitColor, modifier = Modifier.testTag("calorie-deficit-value"))
                Spacer(Modifier.width(6.dp))
                Text("ккал", style = MaterialTheme.typography.titleMedium, color = KkalScanColors.OnSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                Spacer(Modifier.width(8.dp))
                Text(deficitLabel, style = MaterialTheme.typography.titleMedium, color = deficitColor, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
            val details = buildList { if (healthConnectKcal > 0) add("Health Connect: $healthConnectKcal ккал"); if (workoutKcal > 0) add("Тренировки: $workoutKcal ккал"); steps?.let { add("Шаги: $it") } }
            Text(if (details.isNotEmpty()) details.joinToString(" · ") else "Подключите Health Connect или добавьте тренировку", style = MaterialTheme.typography.bodySmall, color = KkalScanColors.OnSurfaceVariant)
        }
    }
}
@Composable private fun BalanceMetric(label: String, value: Int, color: Color) {
    Column { Text(label, style = MaterialTheme.typography.labelMedium, color = KkalScanColors.OnSurfaceVariant); Spacer(Modifier.height(4.dp)); Text("$value", style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold); Text("ккал", style = MaterialTheme.typography.bodySmall, color = KkalScanColors.OnSurfaceVariant) }
}

@Composable
fun DiaryEntryCard(entry: DiaryEntry) {
    val dish = entry.dishes.firstOrNull()
    KkalFoodCard(
        title = dish?.name ?: entry.mealType.label(),
        kcal = entry.totalKcal,
        subtitle = dish?.let { "${it.grams} г" },
        tipBadge = entry.mealType.label(),
        macros = dish?.let { Triple(it.protein, it.fat, it.carbs) },
        fiber = dish?.fiber,
        iconLabel = dishIconLabel(dish?.name),
    )
}

private fun dishIconLabel(name: String?): String =
    name?.firstOrNull()?.uppercaseChar()?.toString() ?: "K"

@Composable
fun KkalPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = KkalScanColors.Primary,
    contentColor: Color = KkalScanColors.OnPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(KkalScanDimens.buttonHeight)
            .shadow(8.dp, RoundedCornerShape(999.dp), ambientColor = containerColor.copy(0.25f)),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
fun KkalEmptyState(
    iconLabel: String,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KkalLargeIllustration(label = iconLabel)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = KkalScanColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            KkalPrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
fun KkalErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFECEC),
        border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Error.copy(0.3f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(message, color = KkalScanColors.Error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            KkalPrimaryButton(
                text = "Повторить",
                onClick = onRetry,
                containerColor = KkalScanColors.Error,
            )
        }
    }
}

@Composable
fun KkalScanHero(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(KkalScanDimens.cardRadius))
            .background(
                Brush.linearGradient(
                    listOf(KkalScanColors.Primary, Color(0xFFFF4D8D)),
                ),
            )
            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(KkalScanDimens.cardRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KkalCameraFrame()
            Spacer(Modifier.height(16.dp))
            Text(
                "Наведите на тарелку",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AI распознает блюда за секунды",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun KkalCameraFrame(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    Canvas(modifier.size(88.dp).then(modifier)) {
        val len = size.minDimension * 0.28f
        val stroke = 4.dp.toPx()
        val inset = size.minDimension * 0.12f
        val corners = listOf(
            Offset(inset, inset),
            Offset(size.width - inset, inset),
            Offset(inset, size.height - inset),
            Offset(size.width - inset, size.height - inset),
        )
        corners.forEachIndexed { index, center ->
            val horizontal = if (index % 2 == 0) 1f else -1f
            val vertical = if (index < 2) 1f else -1f
            drawLine(
                color = color,
                start = center,
                end = Offset(center.x + horizontal * len, center.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = center,
                end = Offset(center.x, center.y + vertical * len),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun ru.kkalscan.domain.model.MealType.label(): String = when (this) {
    ru.kkalscan.domain.model.MealType.breakfast -> "Завтрак"
    ru.kkalscan.domain.model.MealType.lunch -> "Обед"
    ru.kkalscan.domain.model.MealType.dinner -> "Ужин"
    ru.kkalscan.domain.model.MealType.snack -> "Перекус"
}
