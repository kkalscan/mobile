package ru.kkalscan.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.ScanBadge
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.BugReportResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BugReportDialog(
    submitting: Boolean,
    success: BugReportResult?,
    errorMessage: String?,
    screenshots: List<ByteArray>,
    onDismiss: () -> Unit,
    onClearFeedback: () -> Unit,
    onPickScreenshot: () -> Unit,
    onRemoveScreenshot: (Int) -> Unit,
    onSubmit: (email: String, description: String, screenshots: List<ByteArray>) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = {
            if (!submitting) {
                onClearFeedback()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = KkalScanDimens.phoneMaxWidth - 32.dp)
                .fillMaxWidth()
                .testTag("bug-report-dialog"),
            shape = RoundedCornerShape(24.dp),
            color = KkalScanColors.Background,
            shadowElevation = 16.dp,
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Сообщить о баге",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Опишите проблему и укажите email — активируем Pro на месяц.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KkalScanColors.OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                if (success != null) {
                    ScanBadge(text = "Pro активирован")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        success.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = KkalScanColors.Primary,
                    )
                    success.proUntil?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Действует до $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KkalScanColors.OnSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    KkalPrimaryButton(
                        text = "Готово",
                        onClick = {
                            onClearFeedback()
                            onDismiss()
                        },
                    )
                    return@Column
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth().testTag("bug-report-email"),
                    label = { Text("Email *") },
                    singleLine = true,
                    enabled = !submitting,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().testTag("bug-report-description"),
                    label = { Text("Описание бага *") },
                    minLines = 4,
                    enabled = !submitting,
                )
                Spacer(Modifier.height(16.dp))
                Text("Скриншоты", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    screenshots.forEachIndexed { index, _ ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                if (!submitting) onRemoveScreenshot(index)
                            },
                            label = { Text("Скрин ${index + 1} ✕") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KkalScanColors.SecondaryContainer,
                            ),
                        )
                    }
                    if (screenshots.size < 3) {
                        FilterChip(
                            selected = false,
                            onClick = { if (!submitting) onPickScreenshot() },
                            label = { Text("+ Добавить") },
                        )
                    }
                }
                errorMessage?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = KkalScanColors.Error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                KkalPrimaryButton(
                    text = if (submitting) "Отправка…" else "Отправить",
                    onClick = {
                        onSubmit(email.trim(), description.trim(), screenshots)
                    },
                    enabled = !submitting &&
                        email.isNotBlank() &&
                        description.length >= 10,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        if (!submitting) {
                            onClearFeedback()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally),
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}
