package ru.kkalscan.app.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.components.DiaryEntryCard
import ru.kkalscan.app.components.KkalEmptyState
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.components.KkalHeroCard
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.presentation.diary.IDiaryViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun DiaryScreen(
    viewModel: IDiaryViewModel,
    onScanClick: () -> Unit,
    onRefresh: () -> Unit,
    scanErrorMessage: String? = null,
    onRetryScan: () -> Unit = onScanClick,
) {
    val state by viewModel.state.collectAsState()
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val dateLabel = "${today.dayOfMonth}.${today.monthNumber.toString().padStart(2, '0')}.${today.year}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = KkalScanDimens.screenHorizontal),
    ) {
        Spacer(Modifier.height(20.dp))
        KkalPageHeader(
            brand = "KkalScan",
            title = "Сегодня · $dateLabel",
            modifier = Modifier.testTag("diary-title"),
        )
        Spacer(Modifier.height(20.dp))

        scanErrorMessage?.let { message ->
            KkalErrorBanner(message = message, onRetry = onRetryScan)
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.isLoading && state.day == null -> {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = KkalScanColors.Primary)
                }
            }

            state.errorMessage != null -> {
                KkalErrorBanner(message = state.errorMessage!!, onRetry = onRefresh)
            }

            else -> {
                val day = state.day
                val macros = day?.macroTotals()
                KkalHeroCard(
                    title = "СЪЕДЕНО СЕГОДНЯ",
                    kcal = day?.totalKcal ?: 0,
                    subtitle = if ((day?.entries?.size ?: 0) > 0) {
                        "${day?.entries?.size} приёма пищи"
                    } else {
                        "Сфотографируйте еду — AI посчитает ккал и БЖУ"
                    },
                    badge = day?.scansLeft?.let { "Осталось $it скана" },
                    protein = macros?.first ?: 0.0,
                    fat = macros?.second ?: 0.0,
                    carbs = macros?.third ?: 0.0,
                    watermark = "01",
                )
                Spacer(Modifier.height(24.dp))
                Text("Приёмы пищи", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                if (day?.entries.isNullOrEmpty()) {
                    KkalEmptyState(
                        iconLabel = "AI",
                        title = "Дневник пуст",
                        message = "Сфоткайте тарелку — за пару секунд увидите калории и добавите в день",
                        actionLabel = "Сфотографировать еду",
                        onAction = onScanClick,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(day!!.entries, key = { it.id }) { entry ->
                            DiaryEntryCard(entry)
                        }
                    }
                }
            }
        }
    }
}

private fun DiaryDay.macroTotals(): Triple<Double, Double, Double> {
    val dishes = entries.flatMap { it.dishes }
    return Triple(
        dishes.sumOf { it.protein },
        dishes.sumOf { it.fat },
        dishes.sumOf { it.carbs },
    )
}
