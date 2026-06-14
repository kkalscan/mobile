package ru.kkalscan.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CitrusScanColors = lightColorScheme(
    primary = KkalScanColors.Primary,
    onPrimary = KkalScanColors.OnPrimary,
    primaryContainer = KkalScanColors.PrimaryContainer,
    secondary = KkalScanColors.Secondary,
    secondaryContainer = KkalScanColors.SecondaryContainer,
    tertiary = KkalScanColors.Tertiary,
    tertiaryContainer = KkalScanColors.TertiaryContainer,
    background = KkalScanColors.Background,
    surface = KkalScanColors.Surface,
    surfaceVariant = KkalScanColors.SurfaceVariant,
    outline = KkalScanColors.Outline,
    onBackground = KkalScanColors.OnBackground,
    onSurface = KkalScanColors.OnBackground,
    onSurfaceVariant = KkalScanColors.OnSurfaceVariant,
    error = KkalScanColors.Error,
)

@Composable
fun KkalScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CitrusScanColors,
        typography = KkalScanTypography,
        content = content,
    )
}
