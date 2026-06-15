package ru.kkalscan.app.ui.profile

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.components.KkalIconBadge
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.KkalTipCard
import ru.kkalscan.app.components.ScanBadge
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.presentation.profile.IProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: IProfileViewModel,
    onRefresh: () -> Unit,
    onBuyPro: () -> Unit,
    scanErrorMessage: String? = null,
    onRetryScan: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KkalScanDimens.screenHorizontal),
    ) {
        Spacer(Modifier.height(20.dp))
        KkalPageHeader(
            brand = "KkalScan",
            title = "Профиль",
            modifier = Modifier.testTag("profile-title"),
        )
        Spacer(Modifier.height(20.dp))

        scanErrorMessage?.let { message ->
            KkalErrorBanner(message = message, onRetry = onRetryScan)
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.isLoading && state.status == null -> {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = KkalScanColors.Primary)
                }
            }

            state.errorMessage != null -> {
                KkalErrorBanner(message = state.errorMessage!!, onRetry = onRefresh)
            }

            else -> {
                val status = state.status
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    KkalIconBadge(
                        label = if (status?.isPro == true) "Pro" else "G",
                        background = if (status?.isPro == true) KkalScanColors.ProContainer else KkalScanColors.SecondaryContainer,
                        foreground = if (status?.isPro == true) KkalScanColors.Pro else KkalScanColors.Secondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        if (status?.isPro == true) "KkalScan Pro" else "Гость",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        if (status?.accountLinked == true) {
                            "Аккаунт привязан: ${status.linkedProviders.joinToString(", ")}"
                        } else {
                            "Без регистрации · device_id"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = KkalScanColors.OnSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
                state.scansLeft?.let {
                    ScanBadge(
                        if (status?.isPro == true) "Безлимитные сканы" else "Осталось $it скана сегодня",
                    )
                }
                Spacer(Modifier.height(20.dp))
                if (status?.isPro != true) {
                    KkalTipCard(
                        number = "Pro",
                        title = "KkalScan Pro — 199 ₽/мес",
                        body = "Безлимитные сканы каждый день и сохранение истории в облаке",
                    )
                    Spacer(Modifier.height(12.dp))
                    KkalPrimaryButton(
                        text = "Оформить Pro",
                        onClick = onBuyPro,
                        containerColor = KkalScanColors.Pro,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                KkalTipCard(
                    number = "01",
                    title = "Скан через +",
                    body = "Нажмите оранжевую кнопку внизу — выберите фото еды и добавьте результат в дневник",
                )
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}
