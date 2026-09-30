package org.daviante.kromium.sample.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Minimalist, high-contrast monochrome (pure black and white) palette
private val DarkBackground = Color(0xFF0A0A0A)
private val DarkSurface = Color(0xFF141414)
private val DarkSurfaceVariant = Color(0xFF1F1F1F)
private val DarkBorder = Color(0xFF333333)
private val DarkPrimary = Color(0xFFFFFFFF)
private val DarkSecondary = Color(0xFFCCCCCC)
private val DarkTertiary = Color(0xFF999999)
private val DarkTextPrimary = Color(0xFFF0F0F0)
private val DarkTextSecondary = Color(0xFFAAAAAA)
private val DarkError = Color(0xFFE53935)

private val LightBackground = Color(0xFFFFFFFF)
private val LightSurface = Color(0xFFF8F8F8)
private val LightSurfaceVariant = Color(0xFFEFEFEF)
private val LightBorder = Color(0xFFCCCCCC)
private val LightPrimary = Color(0xFF000000)
private val LightSecondary = Color(0xFF333333)
private val LightTertiary = Color(0xFF666666)
private val LightTextPrimary = Color(0xFF111111)
private val LightTextSecondary = Color(0xFF555555)
private val LightError = Color(0xFFD32F2F)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    secondary = DarkSecondary,
    onSecondary = Color.Black,
    tertiary = DarkTertiary,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF282828),
    error = DarkError
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFE0E0E0),
    error = LightError
)

@Composable
fun KromiumSampleTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
