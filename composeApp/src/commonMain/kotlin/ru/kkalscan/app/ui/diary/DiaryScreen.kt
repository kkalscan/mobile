package ru.kkalscan.app.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.components.DiaryEntryCard
import ru.kkalscan.app.components.KkalCalorieBalanceCard
import ru.kkalscan.app.components.KkalEmptyState
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.components.KkalFoodCard
import ru.kkalscan.app.components.KkalHeroCard
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.DiaryDay
import ru.kkalscan.domain.model.WorkoutEntry
import ru.kkalscan.presentation.diary.IDiaryViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun DiaryScreen(
    viewModel: IDiaryViewModel,
    onScanClick: () -> Unit,
    onRequestHealthConnect: () -> Unit,
    onRefresh: () -> Unit,
    scanErrorMessage: String? = null,
    onRetryScan: () -> Unit = onScanClick,
) {
    val state by viewModel.state.collectAsState()
    val today = state.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val dateLabel = "${today.dayOfMonth}.${today.monthNumber.toString().padStart(2, '0')}.${today.year}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KkalScanDimens.screenHorizontal),
    ) {
        Spacer(Modifier.height(20.dp))
        KkalPageHeader(title = "Сегодня · $dateLabel", modifier = Modifier.testTag("diary-title"))
        Spacer(Modifier.height(20.dp))
        scanErrorMessage?.let { message ->
            KkalErrorBanner(message = message, onRetry = onRetryScan)
            Spacer(Modifier.height(12.dp))
        }
        when {
            state.isLoading && state.day == null -> {
                Column(Modifier.fillMaxWidth().height(200.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = KkalScanColors.Primary)
                }
            }
            state.errorMessage != null -> KkalErrorBanner(message = state.errorMessage!!, onRetry = onRefresh)
            else -> {
                val day = state.day
                val balance = state.balance
                val macros = day?.macroTotals()
                if (balance != null) {
                    KkalCalorieBalanceCard(balance.eatenKcal, balance.burnedKcal, balance.deficitKcal, balance.healthConnectKcal, balance.workoutKcal, state.steps)
                    Spacer(Modifier.height(16.dp))
                }
                if (!day?.workouts.isNullOrEmpty()) {
                    Text("Тренировки", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        day!!.workouts.forEach { WorkoutEntryCard(it) }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (state.healthConnectAvailable && !state.healthConnectPermissionsGranted) {
                    OutlinedButton(onClick = onRequestHealthConnect, modifier = Modifier.fillMaxWidth().testTag("health-connect-request")) {
                        Text("Подключить Health Connect")
                    }
                    Spacer(Modifier.height(16.dp))
                }
                KkalHeroCard(
                    title = "СЪЕДЕНО СЕГОДНЯ", kcal = day?.totalKcal ?: 0,
                    subtitle = if ((day?.entries?.size ?: 0) > 0) "${day?.entries?.size} приёма пищи" else "Сфотографируйте еду — AI посчитает ккал и БЖУ",
                    badge = day?.scansLeft?.let { "Осталось $it скана" },
                    protein = macros?.protein ?: 0.0, fat = macros?.fat ?: 0.0, carbs = macros?.carbs ?: 0.0, fiber = macros?.fiber ?: 0.0, watermark = null,
                )
                Spacer(Modifier.height(24.dp))
                Text("Приёмы пищи", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                if (day?.entries.isNullOrEmpty()) {
                    KkalEmptyState(iconLabel = "AI", title = "Дневник пуст", message = "Сфоткайте тарелку — за пару секунд увидите калории и добавите в день")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { day!!.entries.forEach { DiaryEntryCard(it) } }
                }
                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable private fun WorkoutEntryCard(entry: WorkoutEntry) {
    KkalFoodCard(entry.name, entry.kcal, "Сожжено", tipBadge = "Тренировка", iconLabel = "W")
}
private data class MacroSummary(val protein: Double, val fat: Double, val carbs: Double, val fiber: Double)
private fun DiaryDay.macroTotals() = MacroSummary(
    entries.flatMap { it.dishes }.sumOf { it.protein },
    entries.flatMap { it.dishes }.sumOf { it.fat },
    entries.flatMap { it.dishes }.sumOf { it.carbs },
    entries.flatMap { it.dishes }.sumOf { it.fiber },
)
