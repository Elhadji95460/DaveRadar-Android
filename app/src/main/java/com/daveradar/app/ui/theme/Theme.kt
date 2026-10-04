package com.daveradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette « Dave Radar » — sombre premium + vert néon
val Neon = Color(0xFF00FF9F)
val UwbCyan = Color(0xFF00E0FF)
val RadarBg = Color(0xFF040A14)
val RadarNavy = Color(0xFF0A1626)
val CardBg = Color(0xFF0E1D30)
val TextDim = Color(0xB3FFFFFF)     // blanc 70 %
val TextFaint = Color(0x66FFFFFF)   // blanc 40 %

private val DarkColors = darkColorScheme(
    primary = Neon,
    onPrimary = Color.Black,
    secondary = UwbCyan,
    onSecondary = Color.Black,
    background = RadarBg,
    onBackground = Color.White,
    surface = RadarNavy,
    onSurface = Color.White,
    surfaceVariant = CardBg,
    onSurfaceVariant = TextDim,
    outline = Color(0x33FFFFFF)
)

@Composable
fun DaveRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content
    )
}
