package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkAppColors = AppColors(
    ground = DarkGround,
    raised = DarkRaised,
    rule = DarkRule,
    ink = DarkInk,
    muted = DarkMuted,
    present = DarkPresent,
    short = DarkShort,
    danger = DarkDanger,
    isDark = true
)

private val LightAppColors = AppColors(
    ground = LightGround,
    raised = LightRaised,
    rule = LightRule,
    ink = LightInk,
    muted = LightMuted,
    present = LightPresent,
    short = LightShort,
    danger = LightDanger,
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

// Dark Color Scheme: fully specified, no purple-tinted greys, surfaceTint is Transparent
private val DarkColorScheme = darkColorScheme(
    primary = DarkPresent,
    onPrimary = DarkGround,
    primaryContainer = DarkRaised,
    onPrimaryContainer = DarkPresent,
    inversePrimary = LightPresent,
    secondary = DarkPresent,
    onSecondary = DarkGround,
    // Present at ~16% over Raised (0x1E2126 * 0.84 + 0x5BB5A2 * 0.16) for high-visibility navigation indicators
    secondaryContainer = Color(0xFF28393A),
    onSecondaryContainer = DarkPresent,
    tertiary = DarkShort,
    onTertiary = DarkGround,
    tertiaryContainer = DarkRaised,
    onTertiaryContainer = DarkShort,
    background = DarkGround,
    onBackground = DarkInk,
    surface = DarkRaised,
    onSurface = DarkInk,
    surfaceVariant = DarkRaised,
    onSurfaceVariant = DarkMuted,
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFF111315),
    surfaceBright = Color(0xFF282C33),
    surfaceContainerLowest = Color(0xFF0F1012),
    surfaceContainerLow = Color(0xFF181B1F),
    surfaceContainer = Color(0xFF1E2126),
    surfaceContainerHigh = Color(0xFF24282E),
    surfaceContainerHighest = Color(0xFF2B3037),
    inverseSurface = DarkInk,
    inverseOnSurface = DarkGround,
    error = DarkDanger,
    onError = DarkGround,
    errorContainer = DarkRaised,
    onErrorContainer = DarkDanger,
    outline = DarkRule,
    outlineVariant = DarkRule,
    scrim = Color.Black
)

// Light Color Scheme: fully specified, neutral clean tones, surfaceTint is Transparent
private val LightColorScheme = lightColorScheme(
    primary = LightPresent,
    onPrimary = Color.White,
    primaryContainer = LightRaised,
    onPrimaryContainer = LightPresent,
    inversePrimary = DarkPresent,
    secondary = LightPresent,
    onSecondary = Color.White,
    // Present at ~12% over Raised (0xFFFFFF)
    secondaryContainer = Color(0xFFE5EEEC),
    onSecondaryContainer = LightPresent,
    tertiary = LightShort,
    onTertiary = Color.White,
    tertiaryContainer = LightRaised,
    onTertiaryContainer = LightShort,
    background = LightGround,
    onBackground = LightInk,
    surface = LightRaised,
    onSurface = LightInk,
    surfaceVariant = LightRaised,
    onSurfaceVariant = LightMuted,
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE9EBEF),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFF1F3F5),
    surfaceContainerHigh = Color(0xFFEBEEF2),
    surfaceContainerHighest = Color(0xFFE2E5EA),
    inverseSurface = LightInk,
    inverseOnSurface = Color(0xFFECEDEF),
    error = LightDanger,
    onError = Color.White,
    errorContainer = LightRaised,
    onErrorContainer = LightDanger,
    outline = LightRule,
    outlineVariant = LightRule,
    scrim = Color.Black
)

/**
 * WFO Tracker Theme
 * Defaults to dark theme unless the system is in light mode.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
