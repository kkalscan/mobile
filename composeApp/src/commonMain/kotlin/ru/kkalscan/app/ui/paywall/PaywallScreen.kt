package ru.kkalscan.app.ui.paywall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.KkalTipCard
import ru.kkalscan.app.components.ScanBadge
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens

@Composable
fun PaywallScreen(
    scansLeft: Int?,
    onWatchAd: () -> Unit,
    onBuyPro: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KkalScanDimens.screenHorizontal),
    ) {
        Spacer(Modifier.height(24.dp))
        KkalPageHeader(title = "Сканы закончились")
        Text(
            "Посмотрите короткую рекламу или оформите Pro",
            style = MaterialTheme.typography.bodyLarge,
            color = KkalScanColors.OnSurfaceVariant,
        )
        scansLeft?.let {
            Spacer(Modifier.height(12.dp))
            ScanBadge("Сейчас: $it сканов")
        }
        Spacer(Modifier.height(20.dp))
        KkalTipCard(
            number = "+2",
            title = "Бесплатно за рекламу",
            body = "Короткий ролик — и снова можно сканировать тарелки сегодня",
        )
        Spacer(Modifier.height(12.dp))
        KkalTipCard(
            number = "Pro",
            title = "Без лимитов каждый день",
            body = "Pro снимает ограничение на сканы и сохраняет историю в облаке",
        )
        Spacer(Modifier.height(24.dp))
        KkalPrimaryButton(
            text = "Смотреть рекламу (+2)",
            onClick = onWatchAd,
            containerColor = KkalScanColors.Tertiary,
            contentColor = KkalScanColors.OnBackground,
        )
        Spacer(Modifier.height(12.dp))
        KkalPrimaryButton(
            text = "KkalScan Pro — 15 ₽ (тест)",
            onClick = onBuyPro,
            containerColor = KkalScanColors.Pro,
        )
        Spacer(Modifier.height(12.dp))
        KkalPrimaryButton(
            text = "Вернуться в дневник",
            onClick = onBack,
            containerColor = KkalScanColors.SurfaceVariant,
            contentColor = KkalScanColors.OnBackground,
        )
        Spacer(Modifier.height(100.dp))
    }
}
