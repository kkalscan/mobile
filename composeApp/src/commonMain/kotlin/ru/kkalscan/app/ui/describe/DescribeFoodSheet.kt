package ru.kkalscan.app.ui.describe

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.presentation.scan.IScanViewModel

@Composable
fun DescribeFoodSheet(
    viewModel: IScanViewModel,
    onDismiss: () -> Unit,
    onRecognized: () -> Unit,
    onSubmitDescription: (String) -> Unit,
    initialDescription: String = "",
) {
    val scanState by viewModel.state.collectAsState()
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Prefill from search: don't steal focus / open keyboard until user taps the field.
    var inputFocusEnabled by remember(initialDescription) {
        mutableStateOf(initialDescription.isBlank())
    }

    LaunchedEffect(scanState.result, scanState.isLoading) {
        if (scanState.result != null && !scanState.isLoading) {
            onRecognized()
        }
    }

    LaunchedEffect(initialDescription) {
        if (initialDescription.isBlank()) {
            focusRequester.requestFocus()
        } else {
            keyboardController?.hide()
            delay(100)
            inputFocusEnabled = true
        }
    }

    Dialog(
        onDismissRequest = { if (!scanState.isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = KkalScanDimens.phoneMaxWidth - 32.dp)
                .fillMaxWidth()
                .testTag("describe-food-sheet"),
            shape = RoundedCornerShape(24.dp),
            color = KkalScanColors.Background,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Описать еду",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Напишите, что съели — AI оценит калории, БЖУ и клетчатку",
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
                        .testTag("describe-food-input")
                        .focusRequester(focusRequester)
                        .focusProperties { canFocus = inputFocusEnabled },
                    placeholder = { Text("Например: тарелка борща и кусок хлеба, или 200 г курицы с рисом") },
                    enabled = !scanState.isLoading,
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(12.dp))

                scanState.errorMessage?.let { message ->
                    KkalErrorBanner(
                        message = message,
                        onRetry = { onSubmitDescription(description) },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (scanState.isLoading) {
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
                        modifier = Modifier.fillMaxWidth().testTag("describe-food-submit"),
                    )
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    enabled = !scanState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Закрыть", color = KkalScanColors.OnSurfaceVariant)
                }
            }
        }
    }
}
