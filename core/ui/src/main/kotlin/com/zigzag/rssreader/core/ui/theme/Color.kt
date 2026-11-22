package com.zigzag.rssreader.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Hand-authored indigo/teal palettes expressed entirely through Material 3 semantic roles.
 * All other modules should read colors from [androidx.compose.material3.MaterialTheme.colorScheme]
 * rather than referencing these values directly.
 */
fun softUiLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF4655A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE1FF),
    onPrimaryContainer = Color(0xFF293775),
    inversePrimary = Color(0xFFB4C0FF),
    secondary = Color(0xFF2C7A76),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC0EDE6),
    onSecondaryContainer = Color(0xFF0E504D),
    tertiary = Color(0xFF765A92),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF3B2454),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1A1C23),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1A1C23),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44464F),
    surfaceTint = Color(0xFF4655A4),
    inverseSurface = Color(0xFF2F3037),
    inverseOnSurface = Color(0xFFF2F0F7),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF8C1D18),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C5D0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFBFF),
    surfaceDim = Color(0xFFD9D9E3),
    surfaceContainer = Color(0xFFEEEFF3),
    surfaceContainerHigh = Color(0xFFE6E7EF),
    surfaceContainerHighest = Color(0xFFE0E1EC),
    surfaceContainerLow = Color(0xFFF5F5FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

/** Dark counterpart of [softUiLightColorScheme]. */
fun softUiDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFB4C0FF),
    onPrimary = Color(0xFF1A2770),
    primaryContainer = Color(0xFF2F3E8C),
    onPrimaryContainer = Color(0xFFDCE1FF),
    inversePrimary = Color(0xFF4655A4),
    secondary = Color(0xFF98DAD2),
    onSecondary = Color(0xFF003B39),
    secondaryContainer = Color(0xFF155451),
    onSecondaryContainer = Color(0xFFC0EDE6),
    tertiary = Color(0xFFD4B9EF),
    onTertiary = Color(0xFF3D2256),
    tertiaryContainer = Color(0xFF55396F),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF11121A),
    onBackground = Color(0xFFE3E1EC),
    surface = Color(0xFF11121A),
    onSurface = Color(0xFFE3E1EC),
    surfaceVariant = Color(0xFF41434D),
    onSurfaceVariant = Color(0xFFC4C5D0),
    surfaceTint = Color(0xFFB4C0FF),
    inverseSurface = Color(0xFFE3E1EC),
    inverseOnSurface = Color(0xFF2F3037),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44464F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF373941),
    surfaceDim = Color(0xFF11121A),
    surfaceContainer = Color(0xFF1E1F27),
    surfaceContainerHigh = Color(0xFF292A33),
    surfaceContainerHighest = Color(0xFF34353F),
    surfaceContainerLow = Color(0xFF181920),
    surfaceContainerLowest = Color(0xFF0B0C12),
)
