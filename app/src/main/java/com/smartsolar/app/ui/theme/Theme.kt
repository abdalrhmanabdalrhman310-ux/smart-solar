package com.smartsolar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val SmartSolarLightColorScheme = lightColorScheme(
    primary = SolarGreenPrimary,
    onPrimary = SolarWhite,
    primaryContainer = SolarGreenLight,
    onPrimaryContainer = SolarGreenDark,
    secondary = SolarGreenDark,
    onSecondary = SolarWhite,
    secondaryContainer = SolarGreenLight,
    onSecondaryContainer = SolarGreenDark,
    tertiary = SolarAmberAccent,
    onTertiary = SolarWhite,
    tertiaryContainer = SolarAmberSoft,
    onTertiaryContainer = SolarTextPrimary,
    background = SolarBackground,
    onBackground = SolarTextPrimary,
    surface = SolarWhite,
    onSurface = SolarTextPrimary,
    surfaceVariant = SolarSurfaceSubtle,
    onSurfaceVariant = SolarTextSecondary,
    outline = SolarBorderLight,
    outlineVariant = SolarGraySoft,
    error = SolarRedError,
    onError = SolarWhite,
    errorContainer = SolarRedSoft,
    onErrorContainer = SolarRedError
)

private val SmartSolarDarkColorScheme = darkColorScheme(
    primary = SolarGreenDarkModePrimary,
    onPrimary = SolarTextPrimary,
    primaryContainer = SolarGreenDark,
    onPrimaryContainer = SolarGreenLight,
    secondary = SolarGreenPrimary,
    onSecondary = SolarWhite,
    background = SolarDarkBackground,
    onBackground = SolarWhite,
    surface = SolarDarkSurface,
    onSurface = SolarWhite,
    surfaceVariant = SolarDarkSurfaceVariant,
    onSurfaceVariant = SolarGrayMuted,
    outline = SolarDarkSurfaceVariant,
    error = SolarRedError,
    onError = SolarWhite
)

@Composable
fun SmartSolarTheme(
    darkTheme: Boolean = false, // Light-first clean energy design as specified
    forceRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SmartSolarDarkColorScheme else SmartSolarLightColorScheme
    val layoutDirection = if (forceRtl) LayoutDirection.Rtl else LocalLayoutDirection.current

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SmartSolarTypography,
            shapes = SmartSolarShapes,
            content = content
        )
    }
}
