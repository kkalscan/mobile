package ru.kkalscan.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kkalscan.app.theme.KkalScanColors

enum class KkalNavIconType {
    Today,
    Journal,
    Profile,
}

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
fun KkalNavIcon(type: KkalNavIconType, selected: Boolean) {
    val icon = navIconVector(type, selected)
    val tint = if (selected) KkalScanColors.Primary else KkalScanColors.OnSurfaceVariant

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) KkalScanColors.PrimaryContainer else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (type) {
                KkalNavIconType.Today -> "Сегодня"
                KkalNavIconType.Journal -> "Дневник"
                KkalNavIconType.Profile -> "Профиль"
            },
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun navIconVector(type: KkalNavIconType, selected: Boolean): ImageVector = when (type) {
    KkalNavIconType.Today -> if (selected) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday
    KkalNavIconType.Journal -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
    KkalNavIconType.Profile -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
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
