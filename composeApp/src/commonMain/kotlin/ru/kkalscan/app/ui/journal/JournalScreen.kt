package ru.kkalscan.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.charts.ChartCard
import ru.kkalscan.app.charts.KkalCaloriesBarChart
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.components.KkalHeroCard
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.presentation.journal.IJournalViewModel
import ru.kkalscan.stats.WeekDates

@Composable
fun JournalScreen(
    viewModel: IJournalViewModel,
    onRefresh: () -> Unit,
    onRequestInsight: () -> Unit,
    onNeedPro: () -> Unit,
    onDismissInsight: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val week = state.week

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KkalScanDimens.screenHorizontal)
            .testTag("journal-screen"),
    ) {
        Spacer(Modifier.height(20.dp))
        KkalPageHeader(title = "Дневник")
        Spacer(Modifier.height(16.dp))

        JournalWeekHeader(
            label = WeekDates.formatWeekLabel(state.weekStart),
            onPrevious = { viewModel.previousWeek() },
            onNext = { viewModel.nextWeek() },
        )
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading && week == null -> {
                Column(
                    Modifier.fillMaxWidth().height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = KkalScanColors.Primary)
                }
            }

            state.errorMessage != null -> {
                KkalErrorBanner(message = state.errorMessage!!, onRetry = onRefresh)
            }

            week != null -> {
                KkalHeroCard(
                    title = "СРЕДНЕЕ ЗА НЕДЕЛЮ",
                    kcal = week.avgKcal,
                    subtitle = "${week.daysWithData} дней с данными · всего ${week.totalKcal} ккал",
                    badge = null,
                    watermark = "07",
                )
                Spacer(Modifier.height(16.dp))
                DietitianInsightButton(
                    isPro = week.isPro,
                    loading = state.insightLoading,
                    onClick = {
                        if (week.isPro) onRequestInsight() else onNeedPro()
                    },
                )
                state.insightError?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = KkalScanColors.Error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                ChartCard(
                    title = "Калории по дням",
                    subtitle = if (week.daysWithData > 0) {
                        "Среднее ${week.avgKcal} ккал/день"
                    } else {
                        "Нет записей — сфотографируйте еду"
                    },
                ) {
                    KkalCaloriesBarChart(
                        days = week.days,
                        weekStart = state.weekStart,
                    )
                }
                Spacer(Modifier.height(120.dp))
            }
        }
    }

    state.insight?.let { insight ->
        DietitianInsightSheet(insight = insight, onDismiss = onDismissInsight)
    }
}

@Composable
private fun JournalWeekHeader(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущая неделя")
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующая неделя")
        }
    }
}
