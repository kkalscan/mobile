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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kkalscan.analytics.AnalyticsEvents
import ru.kkalscan.app.analytics.KkalAnalytics
import ru.kkalscan.app.platform.appVersionInfo
import ru.kkalscan.app.components.KkalErrorBanner
import ru.kkalscan.app.components.KkalIconBadge
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.KkalTipCard
import ru.kkalscan.app.components.ScanBadge
import ru.kkalscan.app.platform.PhotoPickSource
import ru.kkalscan.app.platform.rememberPhotoPicker
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.presentation.profile.IProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: IProfileViewModel,
    onRefresh: () -> Unit,
    onBuyPro: () -> Unit,
    onSubmitBugReport: (email: String, description: String, screenshots: List<ByteArray>) -> Unit,
    scanErrorMessage: String? = null,
    onRetryScan: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var showBugReportDialog by remember { mutableStateOf(false) }
    var pendingScreenshots by remember { mutableStateOf(listOf<ByteArray>()) }

    val pickScreenshot = rememberPhotoPicker(PhotoPickSource.Gallery) { bytes ->
        if (bytes != null && pendingScreenshots.size < 3) {
            pendingScreenshots = pendingScreenshots + bytes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KkalScanDimens.screenHorizontal),
    ) {
        Spacer(Modifier.height(20.dp))
        KkalPageHeader(
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
                        number = "",
                        badgeIcon = Icons.Outlined.Star,
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
                    number = "",
                    badgeIcon = Icons.Outlined.BugReport,
                    title = "Нашли баг?",
                    body = "Сообщите об ошибке — подарим Pro на месяц бесплатно",
                    onClick = {
                        KkalAnalytics.reportAction(AnalyticsEvents.BUG_REPORT_OPEN)
                        pendingScreenshots = emptyList()
                        viewModel.clearBugReportFeedback()
                        showBugReportDialog = true
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        val version = remember { appVersionInfo() }
        Text(
            text = "Версия ${version.versionName} (${version.versionCode})",
            style = MaterialTheme.typography.bodySmall,
            color = KkalScanColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("profile-version"),
        )
        Spacer(Modifier.height(120.dp))
    }

    if (showBugReportDialog) {
        BugReportDialog(
            submitting = state.bugReportSubmitting,
            success = state.bugReportSuccess,
            errorMessage = state.bugReportError,
            screenshots = pendingScreenshots,
            onDismiss = {
                showBugReportDialog = false
                pendingScreenshots = emptyList()
            },
            onClearFeedback = { viewModel.clearBugReportFeedback() },
            onPickScreenshot = pickScreenshot,
            onRemoveScreenshot = { index ->
                pendingScreenshots = pendingScreenshots.filterIndexed { i, _ -> i != index }
            },
            onSubmit = { email, description, screenshots ->
                KkalAnalytics.reportAction(AnalyticsEvents.BUG_REPORT_SUBMIT)
                onSubmitBugReport(email, description, screenshots)
            },
        )
    }
}
