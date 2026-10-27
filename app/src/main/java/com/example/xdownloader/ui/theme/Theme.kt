package com.example.xdownloader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Accent = Color(0xFF7AE7CE)
private val DarkBackground = Color(0xFF090E18)
private val DarkSurface = Color(0xFF141D2D)
private val DarkOutline = Color(0xFF344154)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF00352A),
    primaryContainer = Color(0xFF183F3A),
    onPrimaryContainer = Color(0xFFC9FFF1),
    secondary = Color(0xFFBBAEFF),
    onSecondary = Color(0xFF281B57),
    secondaryContainer = Color(0xFF302849),
    onSecondaryContainer = Color(0xFFEAE4FF),
    background = DarkBackground,
    onBackground = Color(0xFFEEF4FF),
    surface = DarkSurface,
    onSurface = Color(0xFFEEF4FF),
    surfaceVariant = Color(0xFF202B3E),
    onSurfaceVariant = Color(0xFFACB9CD),
    outline = DarkOutline,
    error = Color(0xFFFFB4AB)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B58),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFAAEFDF),
    onPrimaryContainer = Color(0xFF00382D),
    secondary = Color(0xFF6652B2),
    background = Color(0xFFF4F7FA),
    onBackground = Color(0xFF152234),
    surface = Color.White,
    onSurface = Color(0xFF152234),
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = Color(0xFF536276),
    outline = Color(0xFFC7D3E0)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun XDownloaderTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content
    )
}
