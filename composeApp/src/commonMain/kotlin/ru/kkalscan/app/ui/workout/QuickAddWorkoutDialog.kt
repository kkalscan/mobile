package ru.kkalscan.app.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens

@Composable
fun QuickAddWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, kcal: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var kcalText by rememberSaveable { mutableStateOf("") }
    val kcal = kcalText.toIntOrNull()
    val canSave = name.trim().length >= 2 && kcal != null && kcal > 0

    Dialog(
        onDismissRequest = onDismiss,
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
                Spacer(Modifier.height(8.dp))
                Text(
                    "Заполните название и калории, чтобы сохранить тренировку в текущем дне.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Название") },
                    placeholder = { Text("Например: бег 5 км") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick-workout-name-input"),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it.filter { ch -> ch.isDigit() } },
                    singleLine = true,
                    label = { Text("Калории") },
                    placeholder = { Text("280") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick-workout-kcal-input"),
                )
                Spacer(Modifier.height(16.dp))
                KkalPrimaryButton(
                    text = "Добавить",
                    onClick = {
                        if (canSave) onConfirm(name.trim(), kcal ?: 0)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick-workout-save"),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Отмена", color = KkalScanColors.OnSurfaceVariant)
                }
            }
        }
    }
}
