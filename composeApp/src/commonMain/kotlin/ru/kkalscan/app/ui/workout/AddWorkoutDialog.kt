package ru.kkalscan.app.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.presentation.workout.IWorkoutViewModel

@Composable
fun AddWorkoutDialog(
    viewModel: IWorkoutViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val result = state.result ?: return

    Dialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = KkalScanDimens.phoneMaxWidth - 32.dp)
                .fillMaxWidth()
                .testTag("add-workout-dialog"),
            shape = RoundedCornerShape(24.dp),
            color = KkalScanColors.Background,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Добавить тренировку?", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text(result.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append("${result.kcal} ккал")
                        result.durationMinutes?.let { append(" · $it мин") }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = KkalScanColors.Primary,
                )
                Spacer(Modifier.height(20.dp))
                KkalPrimaryButton(
                    text = "Добавить в день",
                    onClick = onConfirm,
                    loading = state.isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("add-workout-confirm"),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Отмена", color = KkalScanColors.OnSurfaceVariant)
                }
            }
        }
    }
}
