package ru.kkalscan.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.theme.KkalScanColors

@Composable
fun KkalIconBadge(
    label: String,
    background: Color = KkalScanColors.PrimaryContainer,
    foreground: Color = KkalScanColors.Primary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = foreground, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KkalNavIcon(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) KkalScanColors.PrimaryContainer else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) KkalScanColors.Primary else KkalScanColors.OnSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun KkalLargeIllustration(label: String) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(KkalScanColors.SecondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.displaySmall, color = KkalScanColors.Secondary, fontWeight = FontWeight.Bold)
    }
}
