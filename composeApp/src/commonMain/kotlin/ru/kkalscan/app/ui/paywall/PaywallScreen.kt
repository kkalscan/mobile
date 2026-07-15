package ru.kkalscan.app.ui.paywall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.kkalscan.app.components.KkalPageHeader
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.KkalTipCard
import ru.kkalscan.app.components.ScanBadge
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.model.SubscriptionOffer
import ru.kkalscan.presentation.profile.IProfileViewModel

@Composable
fun PaywallScreen(
    viewModel: IProfileViewModel,
    scansLeft: Int?,
    onBuyPro: (tariff: String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var promoInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadOffers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KkalScanDimens.screenHorizontal),
    ) {
        Spacer(Modifier.height(24.dp))
        KkalPageHeader(title = "Сканы закончились")
        Text(
            "Оформите Pro, чтобы сканировать без дневного лимита",
            style = MaterialTheme.typography.bodyLarge,
            color = KkalScanColors.OnSurfaceVariant,
        )
        scansLeft?.let {
            Spacer(Modifier.height(12.dp))
            ScanBadge("Сейчас: $it сканов")
        }
        Spacer(Modifier.height(20.dp))
        KkalTipCard(
            number = "",
            badgeIcon = Icons.Outlined.Star,
            title = "Без лимитов каждый день",
            body = "Pro снимает ограничение на сканы и сохраняет историю в облаке",
        )
        Spacer(Modifier.height(24.dp))
        PromoCodeSection(
            promoInput = promoInput,
            onPromoInputChange = {
                promoInput = it
                viewModel.clearPromoError()
            },
            applying = state.promoApplying,
            error = state.promoError,
            boundPromoCode = state.boundPromoCode,
            boundDiscountPercent = state.boundDiscountPercent,
            onApply = { scope.launch { viewModel.applyPromo(promoInput) } },
        )
        Spacer(Modifier.height(12.dp))
        OfferButtons(
            offers = state.offers,
            loading = state.offersLoading,
            onBuyPro = onBuyPro,
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

@Composable
internal fun PromoCodeSection(
    promoInput: String,
    onPromoInputChange: (String) -> Unit,
    applying: Boolean,
    error: String?,
    boundPromoCode: String?,
    boundDiscountPercent: Int,
    onApply: () -> Unit,
) {
    if (boundPromoCode != null && boundDiscountPercent > 0) {
        ScanBadge("$boundPromoCode −$boundDiscountPercent%")
        Spacer(Modifier.height(8.dp))
    }
    OutlinedTextField(
        value = promoInput,
        onValueChange = onPromoInputChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Промокод") },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
    )
    Spacer(Modifier.height(8.dp))
    KkalPrimaryButton(
        text = if (applying) "Применяем…" else "Применить",
        onClick = onApply,
        enabled = !applying && promoInput.isNotBlank(),
        containerColor = KkalScanColors.SurfaceVariant,
        contentColor = KkalScanColors.OnBackground,
    )
}

@Composable
internal fun OfferButtons(
    offers: List<SubscriptionOffer>,
    loading: Boolean,
    onBuyPro: (tariff: String) -> Unit,
) {
    if (loading && offers.isEmpty()) {
        Text(
            "Загружаем тарифы…",
            style = MaterialTheme.typography.bodyMedium,
            color = KkalScanColors.OnSurfaceVariant,
        )
        return
    }
    offers.forEach { offer ->
        val label = buildString {
            append(offer.title)
            append(" — ")
            append(offer.amountRub)
            append(" ₽")
        }
        if (offer.discountPercent > 0 && offer.amountRub < offer.priceRub) {
            Text(
                text = "${offer.priceRub} ₽",
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.LineThrough,
                ),
                color = KkalScanColors.OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        KkalPrimaryButton(
            text = label,
            onClick = { onBuyPro(offer.tariff) },
            containerColor = KkalScanColors.Pro,
        )
        Spacer(Modifier.height(12.dp))
    }
}
