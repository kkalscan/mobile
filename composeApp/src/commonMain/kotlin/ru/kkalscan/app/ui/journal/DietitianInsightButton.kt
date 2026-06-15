package ru.kkalscan.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.components.ScanBadge
import ru.kkalscan.app.theme.KkalScanColors

@Composable
fun DietitianInsightButton(
    isPro: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box {
            KkalPrimaryButton(
                text = if (loading) "Анализируем…" else "Анализ диетолога",
                onClick = onClick,
                loading = loading,
                containerColor = KkalScanColors.Pro,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dietitian-insight-button"),
            )
            if (!isPro) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = KkalScanColors.OnPrimary, modifier = Modifier.padding(0.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KkalScanColors.Pro)
            Text(
                if (isPro) "AI-разбор питания за неделю" else "Доступно в Pro · AI-разбор питания",
                style = MaterialTheme.typography.bodyMedium,
                color = KkalScanColors.OnSurfaceVariant,
            )
            if (!isPro) ScanBadge("Pro")
        }
    }
}
