package ru.kkalscan.app.ui.scan

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.KkalScanHero
import ru.kkalscan.app.components.KkalTipCard
import ru.kkalscan.app.components.ScanBadge
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.presentation.scan.IScanViewModel

@Composable
fun ScanScreen(
    viewModel: IScanViewModel,
    onPickPhoto: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KkalScanDimens.screenHorizontal),
    ) {
        Spacer(Modifier.height(20.dp))
        KkalPageHeader(title = "Скан еды")
        Text(
            "AI распознаёт блюда и считает калории",
            style = MaterialTheme.typography.bodyLarge,
            color = KkalScanColors.OnSurfaceVariant,
        )
        state.scansLeft?.let {
            Spacer(Modifier.height(12.dp))
            ScanBadge("Осталось $it бесплатных скана")
        }
        Spacer(Modifier.height(20.dp))
        KkalScanHero()
        Spacer(Modifier.height(20.dp))
        KkalPrimaryButton(
            text = if (state.isLoading) "Распознаём…" else "Выбрать фото",
            onClick = onPickPhoto,
            loading = state.isLoading,
        )
        Spacer(Modifier.height(24.dp))
        KkalTipCard(
            number = "01",
            title = "Сфотографируйте тарелку",
            body = "Сделайте снимок сверху — так AI точнее определит блюда и порции",
        )
        Spacer(Modifier.height(12.dp))
        KkalTipCard(
            number = "02",
            title = "Получите результат",
            body = "Калории, белки, жиры и углеводы появятся за несколько секунд",
        )
        Spacer(Modifier.height(120.dp))
    }
}
