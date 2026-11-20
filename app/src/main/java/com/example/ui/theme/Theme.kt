package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = DoraSkyBlue,
    onPrimary = Color.White,
    primaryContainer = DoraSkyMist,
    onPrimaryContainer = DoraDeepBlue,
    secondary = DoraDoorPink,
    onSecondary = Color.White,
    secondaryContainer = DoraDoorSoft,
    onSecondaryContainer = Color(0xFF9D174D),
    tertiary = DoraBellGold,
    onTertiary = InkDark,
    tertiaryContainer = DoraBellSoft,
    onTertiaryContainer = Color(0xFF78350F),
    error = DoraCollarRed,
    onError = Color.White,
    errorContainer = DoraRedSoft,
    onErrorContainer = Color(0xFF991B1B),
    background = CloudBg,
    onBackground = InkDark,
    surface = CloudSurface,
    onSurface = InkDark,
    surfaceVariant = CloudSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = LightOutline
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF00253A),
    primaryContainer = Color(0xFF004C7A),
    onPrimaryContainer = DoraSkyMist,
    secondary = DoraDoorPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF6B1339),
    onSecondaryContainer = DoraDoorSoft,
    tertiary = DoraBellGold,
    onTertiary = InkDark,
    tertiaryContainer = Color(0xFF5A4200),
    onTertiaryContainer = DoraBellSoft,
    error = Color(0xFFFF4D6D),
    onError = Color.White,
    errorContainer = Color(0xFF660015),
    onErrorContainer = DoraRedSoft,
    background = TokyoNightBg,
    onBackground = TokyoTextPrimary,
    surface = TokyoNightSurface,
    onSurface = TokyoTextPrimary,
    surfaceVariant = TokyoNightSurfaceVariant,
    onSurfaceVariant = TokyoTextMuted,
    outline = TokyoOutline
)

val DoracashShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DoracashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DoracashTypography,
        shapes = DoracashShapes,
        content = content
    )
}
