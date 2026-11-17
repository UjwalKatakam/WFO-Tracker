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
    sheet = DarkSheet,
    rule = DarkRule,
    ink = DarkInk,
    muted = DarkMuted,
    present = DarkPresent,
    presentDeep = DarkPresentDeep,
    onPresent = DarkOnPresent,
    presentContainer = DarkPresentContainer,
    short = DarkShort,
    danger = DarkDanger,
    reward = DarkReward,
    isDark = true
)

private val LightAppColors = AppColors(
    ground = LightGround,
    raised = LightRaised,
    sheet = LightSheet,
    rule = LightRule,
    ink = LightInk,
    muted = LightMuted,
    present = LightPresent,
    presentDeep = LightPresentDeep,
    onPresent = LightOnPresent,
    presentContainer = LightPresentContainer,
    short = LightShort,
    danger = LightDanger,
    reward = LightReward,
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPresent,
    onPrimary = DarkOnPresent,
    primaryContainer = DarkPresentContainer,
    onPrimaryContainer = DarkPresentDeep,
    inversePrimary = LightPresentDeep,
    secondary = DarkPresent,
    onSecondary = DarkOnPresent,
    secondaryContainer = DarkPresentContainer,
    onSecondaryContainer = DarkPresentDeep,
    tertiary = DarkShort,
    onTertiary = DarkGround,
    tertiaryContainer = DarkRaised,
    onTertiaryContainer = DarkShort,
    background = DarkGround,
    onBackground = DarkInk,
    surface = DarkGround,
    onSurface = DarkInk,
    surfaceVariant = DarkRaised,
    onSurfaceVariant = DarkMuted,
    surfaceTint = Color.Transparent,
    surfaceDim = DarkGround,
    surfaceBright = DarkSheet,
    surfaceContainerLowest = DarkGround,
    surfaceContainerLow = DarkGround,
    surfaceContainer = DarkRaised,
    surfaceContainerHigh = DarkSheet,
    surfaceContainerHighest = DarkSheet,
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

private val LightColorScheme = lightColorScheme(
    primary = LightPresent,
    onPrimary = LightOnPresent,
    primaryContainer = LightPresentContainer,
    onPrimaryContainer = LightPresentDeep,
    inversePrimary = DarkPresent,
    secondary = LightPresent,
    onSecondary = LightOnPresent,
    secondaryContainer = LightPresentContainer,
    onSecondaryContainer = LightPresentDeep,
    tertiary = LightShort,
    onTertiary = Color.White,
    tertiaryContainer = LightRaised,
    onTertiaryContainer = LightShort,
    background = LightGround,
    onBackground = LightInk,
    surface = LightGround,
    onSurface = LightInk,
    surfaceVariant = LightRaised,
    onSurfaceVariant = LightMuted,
    surfaceTint = Color.Transparent,
    surfaceDim = LightGround,
    surfaceBright = LightSheet,
    surfaceContainerLowest = LightSheet,
    surfaceContainerLow = LightGround,
    surfaceContainer = LightRaised,
    surfaceContainerHigh = LightSheet,
    surfaceContainerHighest = LightSheet,
    inverseSurface = LightInk,
    inverseOnSurface = LightGround,
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
