package ru.kkalscan.app.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.presentation.diary.IDiaryViewModel

@Composable
fun QuickAddWorkoutDialog(
    viewModel: IDiaryViewModel,
    onDismiss: () -> Unit,
    onSubmitDescription: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val diaryState by viewModel.state.collectAsState()
    val workoutParse = diaryState.workoutParse
    var description by rememberSaveable { mutableStateOf("") }
    val preview = workoutParse.preview
    val isBusy = workoutParse.isLoading

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = KkalScanDimens.phoneMaxWidth - 32.dp)
                .fillMaxWidth()
                .testTag("quick-workout-dialog"),
            shape = RoundedCornerShape(24.dp),
            color = KkalScanColors.Background,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Добавить тренировку", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (preview == null) {
                        "Опишите активность и длительность — AI оценит калории"
                    } else {
                        "Проверьте результат и добавьте в дневник"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                if (preview == null) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("quick-workout-description-input"),
                        placeholder = { Text("Например: бег 30 минут, йога 45 мин") },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                    )
                } else {
                    WorkoutPreviewCard(
                        title = preview.title,
                        burnedKcal = preview.burnedKcal,
                        durationMinutes = preview.durationMinutes,
                    )
                }

                Spacer(Modifier.height(12.dp))

                workoutParse.errorMessage?.let { message ->
                    KkalErrorBanner(
                        message = message,
                        onRetry = {
                            if (preview == null) {
                                onSubmitDescription(description)
                            } else {
                                onConfirm()
                            }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (isBusy) {
                    Row(
                        Modifier.fillMaxWidth().height(52.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = KkalScanColors.Primary)
                    }
                } else if (preview == null) {
                    KkalPrimaryButton(
                        text = "Посчитать",
                        onClick = { onSubmitDescription(description) },
                        enabled = description.trim().length >= 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick-workout-parse"),
                    )
                } else {
                    KkalPrimaryButton(
                        text = "Добавить",
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick-workout-save"),
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.clearWorkoutParse() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Изменить описание", color = KkalScanColors.OnSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Отмена", color = KkalScanColors.OnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WorkoutPreviewCard(
    title: String,
    burnedKcal: Int,
    durationMinutes: Int?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick-workout-preview"),
        shape = RoundedCornerShape(16.dp),
        color = KkalScanColors.Surface,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    append("−$burnedKcal ккал")
                    durationMinutes?.let { append(" · $it мин") }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = KkalScanColors.Primary,
            )
        }
    }
}
