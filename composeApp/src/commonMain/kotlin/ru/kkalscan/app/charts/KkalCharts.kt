package ru.kkalscan.app.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.stats.DayMetrics
import ru.kkalscan.stats.WeekDates

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
    val maxKcal = (days.maxOfOrNull { it.kcal } ?: 1).coerceAtLeast(800).toFloat()
    val labels = days.map { WeekDates.shortDayLabel(it.date, weekStart) }

    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(height)) {
            Canvas(Modifier.fillMaxSize()) {
                val barCount = days.size.coerceAtLeast(1)
                val gap = size.width * 0.04f
                val barWidth = (size.width - gap * (barCount + 1)) / barCount
                val chartBottom = size.height * 0.92f
                val chartTop = size.height * 0.08f
                val chartHeight = chartBottom - chartTop

                days.forEachIndexed { index, day ->
                    val left = gap + index * (barWidth + gap)
                    val ratio = day.kcal / maxKcal
                    val barHeight = chartHeight * ratio.coerceIn(0f, 1f)
                    val top = chartBottom - barHeight
                    val baseColor = if (day.hasData) KkalScanColors.Primary else KkalScanColors.Outline.copy(0.35f)
                    val brush = if (day.hasData) {
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF9F5A), KkalScanColors.Primary, Color(0xFFE85D04)),
                            startY = top,
                            endY = chartBottom,
                        )
                    } else {
                        Brush.verticalGradient(listOf(baseColor, baseColor))
                    }
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(left, top.coerceAtMost(chartBottom - 6f)),
                        size = Size(barWidth, barHeight.coerceAtLeast(if (day.hasData) 8f else 4f)),
                        cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
                    )
                    if (day.hasData) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.25f),
                            topLeft = Offset(left + barWidth * 0.15f, top + 4f),
                            size = Size(barWidth * 0.25f, (barHeight * 0.35f).coerceAtLeast(6f)),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun KkalStackedMacroChart(
    days: List<DayMetrics>,
    weekStart: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp,
) {
    val maxGrams = days.maxOfOrNull { it.protein + it.fat + it.carbs }?.toFloat()?.coerceAtLeast(50f) ?: 100f
    val labels = days.map { WeekDates.shortDayLabel(it.date, weekStart) }
    val proteinColor = KkalScanColors.Protein
    val fatColor = KkalScanColors.Fat
    val carbsColor = KkalScanColors.Carbs

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MacroLegendDot("Б", proteinColor)
            MacroLegendDot("Ж", fatColor)
            MacroLegendDot("У", carbsColor)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(height)) {
            Canvas(Modifier.fillMaxSize()) {
                val barCount = days.size.coerceAtLeast(1)
                val gap = size.width * 0.04f
                val barWidth = (size.width - gap * (barCount + 1)) / barCount
                val chartBottom = size.height * 0.95f
                val chartTop = size.height * 0.05f
                val chartHeight = chartBottom - chartTop

                days.forEachIndexed { index, day ->
                    val left = gap + index * (barWidth + gap)
                    var bottom = chartBottom
                    listOf(
                        day.protein to proteinColor,
                        day.fat to fatColor,
                        day.carbs to carbsColor,
                    ).forEach { (grams, color) ->
                        val h = chartHeight * (grams.toFloat() / maxGrams)
                        if (h > 0f) {
                            drawRect(
                                color = color,
                                topLeft = Offset(left, bottom - h),
                                size = Size(barWidth, h),
                            )
                            bottom -= h
                        }
                    }
                    if (!day.hasData) {
                        drawRoundRect(
                            color = KkalScanColors.SurfaceVariant,
                            topLeft = Offset(left, chartBottom - 6f),
                            size = Size(barWidth, 6f),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

@Composable
fun KkalMacroDonutChart(
    protein: Double,
    fat: Double,
    carbs: Double,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 160.dp,
) {
    val total = (protein + fat + carbs).coerceAtLeast(1.0)
    val segments = listOf(
        protein to KkalScanColors.Protein,
        fat to KkalScanColors.Fat,
        carbs to KkalScanColors.Carbs,
    )
    var startAngle = -90f

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.toPx() * 0.14f
            val radius = this.size.minDimension / 2f - stroke
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(color = KkalScanColors.SurfaceVariant, radius = radius, center = center, style = Stroke(stroke))
            segments.forEach { (value, color) ->
                val sweep = (value / total * 360f).toFloat()
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("БЖУ", style = MaterialTheme.typography.labelLarge, color = KkalScanColors.OnSurfaceVariant)
            Text(
                "${(protein / total * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = KkalScanColors.Protein,
            )
            Text("белок", style = MaterialTheme.typography.labelSmall, color = KkalScanColors.OnSurfaceVariant)
        }
    }
}
