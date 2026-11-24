package com.example.spendsense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SpendSenseShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Green10,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Green90,
    onSecondaryContainer = Green10,
    tertiary = Amber40,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = SoftGray,
    onSurfaceVariant = MutedInk,
    outline = Outline
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green10,
    onPrimaryContainer = Green90,
    secondary = Green80,
    onSecondary = Green20,
    secondaryContainer = Green10,
    onSecondaryContainer = Green90,
    tertiary = Amber80,
    onTertiary = Ink,
    tertiaryContainer = Amber40,
    onTertiaryContainer = Amber90,
    background = NightBg,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightVariant,
    onSurfaceVariant = NightMuted,
    outline = Outline
)

@Composable
fun SpendSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic (wallpaper) color is intentionally OFF so the app keeps its brand green.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = SpendSenseShapes,
        content = content
    )
}
