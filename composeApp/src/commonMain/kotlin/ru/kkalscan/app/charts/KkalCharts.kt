package ru.kkalscan.app.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.stats.DayMetrics
import ru.kkalscan.stats.StatsAggregator
import ru.kkalscan.stats.WeekDates
import kotlin.math.roundToInt

private val yAxisWidth = 44.dp

@Composable
fun ChartCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(KkalScanDimens.cardRadius)),
        shape = RoundedCornerShape(KkalScanDimens.cardRadius),
        color = KkalScanColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Outline.copy(0.45f)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = KkalScanColors.OnSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun KkalCaloriesBarChart(
    days: List<DayMetrics>,
    weekStart: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp,
) {
    val dataMax = days.maxOfOrNull { maxOf(it.kcal, it.burnedKcal) } ?: 0
    val maxKcal = niceKcalMax(dataMax).toFloat()
    val yTicks = yTicksForMax(maxKcal.toInt())
    val labels = days.map { WeekDates.shortDayLabel(it.date, weekStart) }
    val calorieSeries = listOf(
        CalorieSeries("Поступление", { it.kcal }, KkalScanColors.Primary, intakeGradient = true),
        CalorieSeries("Расход", { it.burnedKcal }, KkalScanColors.Protein, intakeGradient = false),
    )

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            calorieSeries.forEach { MacroLegendDot(it.label, it.color) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(height)) {
            YAxisLabels(ticks = yTicks, unit = "ккал")
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxSize()) {
                    val dayCount = days.size.coerceAtLeast(1)
                    val groupGap = size.width * 0.05f
                    val groupWidth = (size.width - groupGap * (dayCount + 1)) / dayCount
                    val innerGap = groupWidth * 0.12f
                    val barWidth = ((groupWidth - innerGap) / 2f).coerceAtLeast(4f)
                    val chartBottom = size.height * 0.96f
                    val chartTop = size.height * 0.04f
                    val chartHeight = chartBottom - chartTop

                    yTicks.forEach { tick ->
                        val y = chartBottom - chartHeight * (tick / maxKcal)
                        drawLine(
                            color = KkalScanColors.Outline.copy(0.35f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                    }

                    days.forEachIndexed { dayIndex, day ->
                        val groupLeft = groupGap + dayIndex * (groupWidth + groupGap)
                        calorieSeries.forEachIndexed { seriesIndex, series ->
                            val kcal = series.value(day).toFloat()
                            val left = groupLeft + seriesIndex * (barWidth + innerGap)
                            val barHeight = chartHeight * (kcal / maxKcal)
                            val top = chartBottom - barHeight
                            val hasValue = kcal > 0f
                            val brush = when {
                                !day.hasData -> Brush.verticalGradient(
                                    listOf(KkalScanColors.Outline.copy(0.35f), KkalScanColors.Outline.copy(0.35f)),
                                )
                                series.intakeGradient && hasValue -> Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFF9F5A), KkalScanColors.Primary, Color(0xFFE85D04)),
                                    startY = top,
                                    endY = chartBottom,
                                )
                                hasValue -> Brush.verticalGradient(
                                    colors = listOf(Color(0xFF5FD4A4), KkalScanColors.Protein, Color(0xFF0D8A57)),
                                    startY = top,
                                    endY = chartBottom,
                                )
                                else -> Brush.verticalGradient(
                                    listOf(series.color.copy(0.35f), series.color.copy(0.35f)),
                                )
                            }
                            drawRoundRect(
                                brush = brush,
                                topLeft = Offset(left, top.coerceAtMost(chartBottom - if (day.hasData) 6f else 4f)),
                                size = Size(barWidth, barHeight.coerceAtLeast(if (day.hasData && hasValue) 6f else 4f)),
                                cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(yAxisWidth))
            labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun KkalFiberBarChart(
    days: List<DayMetrics>,
    weekStart: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 180.dp,
) {
    val dataMax = days.maxOfOrNull { it.fiber.toFloat() } ?: 0f
    val maxGrams = niceGramsMax(dataMax).toFloat()
    val yTicks = yTicksForMax(maxGrams.toInt())
    val labels = days.map { WeekDates.shortDayLabel(it.date, weekStart) }

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(yAxisWidth))
            days.forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                    if (day.fiber > 0) {
                        Text(
                            "${day.fiber.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = KkalScanColors.Fiber,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(height)) {
            YAxisLabels(ticks = yTicks, unit = "г")
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxSize()) {
                    val barCount = days.size.coerceAtLeast(1)
                    val gap = size.width * 0.06f
                    val barWidth = (size.width - gap * (barCount + 1)) / barCount
                    val chartBottom = size.height * 0.96f
                    val chartTop = size.height * 0.04f
                    val chartHeight = chartBottom - chartTop

                    yTicks.forEach { tick ->
                        val y = chartBottom - chartHeight * (tick / maxGrams)
                        drawLine(
                            color = KkalScanColors.Outline.copy(0.35f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                    }

                    days.forEachIndexed { index, day ->
                        val left = gap + index * (barWidth + gap)
                        val ratio = (day.fiber / maxGrams).toFloat()
                        val barHeight = chartHeight * ratio.coerceIn(0f, 1f)
                        val top = chartBottom - barHeight
                        val baseColor = if (day.hasData) KkalScanColors.Fiber else KkalScanColors.Outline.copy(0.35f)
                        val brush = if (day.hasData && day.fiber > 0) {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF5FD4A4), KkalScanColors.Fiber, Color(0xFF0D8A57)),
                                startY = top,
                                endY = chartBottom,
                            )
                        } else {
                            Brush.verticalGradient(listOf(baseColor, baseColor))
                        }
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(left, top.coerceAtMost(chartBottom - 6f)),
                            size = Size(barWidth, barHeight.coerceAtLeast(if (day.hasData && day.fiber > 0) 8f else 4f)),
                            cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(yAxisWidth))
            labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun KkalGroupedMacroChart(
    days: List<DayMetrics>,
    weekStart: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp,
) {
    val dataMax = days.maxOfOrNull { maxOf(it.protein, it.fat, it.carbs).toFloat() } ?: 0f
    val maxGrams = niceGramsMax(dataMax).toFloat()
    val yTicks = yTicksForMax(maxGrams.toInt())
    val labels = days.map { WeekDates.shortDayLabel(it.date, weekStart) }
    val macroSeries = listOf(
        MacroSeries("Б", { it.protein }, KkalScanColors.Protein),
        MacroSeries("Ж", { it.fat }, KkalScanColors.Fat),
        MacroSeries("У", { it.carbs }, KkalScanColors.Carbs),
    )

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            macroSeries.forEach { MacroLegendDot(it.label, it.color) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(height)) {
            YAxisLabels(ticks = yTicks, unit = "г")
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxSize()) {
                    val dayCount = days.size.coerceAtLeast(1)
                    val groupGap = size.width * 0.05f
                    val groupWidth = (size.width - groupGap * (dayCount + 1)) / dayCount
                    val innerGap = groupWidth * 0.07f
                    val barWidth = ((groupWidth - innerGap * 2) / 3f).coerceAtLeast(4f)
                    val chartBottom = size.height * 0.96f
                    val chartTop = size.height * 0.04f
                    val chartHeight = chartBottom - chartTop

                    yTicks.forEach { tick ->
                        val y = chartBottom - chartHeight * (tick / maxGrams)
                        drawLine(
                            color = KkalScanColors.Outline.copy(0.35f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                    }

                    days.forEachIndexed { dayIndex, day ->
                        val groupLeft = groupGap + dayIndex * (groupWidth + groupGap)
                        macroSeries.forEachIndexed { macroIndex, series ->
                            val grams = series.value(day).toFloat()
                            val left = groupLeft + macroIndex * (barWidth + innerGap)
                            val barHeight = chartHeight * (grams / maxGrams)
                            val top = chartBottom - barHeight
                            val color = if (day.hasData) series.color else KkalScanColors.Outline.copy(0.35f)
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(left, top.coerceAtMost(chartBottom - if (day.hasData) 6f else 4f)),
                                size = Size(barWidth, barHeight.coerceAtLeast(if (day.hasData && grams > 0f) 6f else 4f)),
                                cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(yAxisWidth))
            labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun KkalMacroBalanceDonut(
    protein: Double,
    fat: Double,
    carbs: Double,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 168.dp,
) {
    val split = StatsAggregator.macroKcalSplit(protein, fat, carbs)
    val segments = listOf(
        Triple("Белки", split.proteinPercent, KkalScanColors.Protein),
        Triple("Жиры", split.fatPercent, KkalScanColors.Fat),
        Triple("Углев.", split.carbsPercent, KkalScanColors.Carbs),
    )
    var startAngle = -90f

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = size.toPx() * 0.13f
                val radius = this.size.minDimension / 2f - stroke
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                drawCircle(
                    color = KkalScanColors.SurfaceVariant,
                    radius = radius,
                    center = center,
                    style = Stroke(stroke),
                )
                segments.forEach { (_, percent, color) ->
                    val sweep = percent / 100f * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                        startAngle += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${split.totalKcal.roundToInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KkalScanColors.OnBackground,
                )
                Text("ккал из БЖУ", style = MaterialTheme.typography.labelSmall, color = KkalScanColors.OnSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MacroBalanceRow("Белки", split.proteinPercent, protein, KkalScanColors.Protein)
            MacroBalanceRow("Жиры", split.fatPercent, fat, KkalScanColors.Fat)
            MacroBalanceRow("Углев.", split.carbsPercent, carbs, KkalScanColors.Carbs)
        }
    }
}

@Composable
private fun MacroBalanceRow(label: String, percent: Int, grams: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            Text(
                "$percent% · ${grams.roundToInt()} г",
                style = MaterialTheme.typography.bodySmall,
                color = KkalScanColors.OnSurfaceVariant,
            )
        }
    }
}

private data class MacroSeries(
    val label: String,
    val value: (DayMetrics) -> Double,
    val color: Color,
)

private data class CalorieSeries(
    val label: String,
    val value: (DayMetrics) -> Int,
    val color: Color,
    val intakeGradient: Boolean,
)

@Composable
private fun YAxisLabels(ticks: List<Int>, unit: String) {
    Column(
        Modifier
            .width(yAxisWidth)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            unit,
            style = MaterialTheme.typography.labelSmall,
            color = KkalScanColors.OnSurfaceVariant.copy(0.8f),
            modifier = Modifier.padding(end = 6.dp, bottom = 4.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            ticks.reversed().forEach { tick ->
                Text(
                    text = "$tick",
                    style = MaterialTheme.typography.labelSmall,
                    color = KkalScanColors.OnSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MacroLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun niceKcalMax(maxValue: Int): Int {
    if (maxValue <= 0) return 500
    val steps = listOf(300, 500, 800, 1000, 1200, 1500, 2000, 2500, 3000, 4000)
    return steps.firstOrNull { it >= maxValue } ?: (((maxValue + 499) / 500) * 500)
}

private fun niceGramsMax(maxValue: Float): Int {
    if (maxValue <= 0f) return 100
    val v = maxValue.toInt()
    val steps = listOf(50, 100, 150, 200, 250, 300, 400, 500, 600, 800)
    return steps.firstOrNull { it >= v } ?: (((v + 49) / 50) * 50)
}

private fun yTicksForMax(max: Int): List<Int> = when {
    max <= 0 -> listOf(0)
    max <= 300 -> listOf(0, max / 2, max)
    else -> listOf(0, max / 2, max)
}
