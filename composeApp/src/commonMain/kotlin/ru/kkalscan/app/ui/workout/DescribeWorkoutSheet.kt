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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ru.kkalscan.presentation.workout.IWorkoutViewModel

@Composable
fun DescribeWorkoutSheet(
    viewModel: IWorkoutViewModel,
    onDismiss: () -> Unit,
    onRecognized: () -> Unit,
    onSubmitDescription: (String) -> Unit,
) {
    val workoutState by viewModel.state.collectAsState()
    var description by remember { mutableStateOf("") }

    LaunchedEffect(workoutState.result, workoutState.isLoading) {
        if (workoutState.result != null && !workoutState.isLoading) {
            onRecognized()
        }
    }

    Dialog(
        onDismissRequest = { if (!workoutState.isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = KkalScanDimens.phoneMaxWidth - 32.dp)
                .fillMaxWidth()
                .testTag("describe-workout-sheet"),
            shape = RoundedCornerShape(24.dp),
            color = KkalScanColors.Background,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Описать тренировку",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Напишите, что делали — AI оценит сожжённые калории",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("describe-workout-input"),
                    placeholder = { Text("Например: бег 5 км, силовая 45 минут, плавание") },
                    enabled = !workoutState.isLoading,
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(12.dp))

                workoutState.errorMessage?.let { message ->
                    KkalErrorBanner(
                        message = message,
                        onRetry = { onSubmitDescription(description) },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (workoutState.isLoading) {
                    Row(
                        Modifier.fillMaxWidth().height(52.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = KkalScanColors.Primary)
                    }
                } else {
                    KkalPrimaryButton(
                        text = "Посчитать",
                        onClick = { onSubmitDescription(description) },
                        enabled = description.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth().testTag("describe-workout-submit"),
                    )
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    enabled = !workoutState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Закрыть", color = KkalScanColors.OnSurfaceVariant)
                }
            }
        }
    }
}
