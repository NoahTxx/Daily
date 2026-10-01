package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.util.LocalAccentColor
import com.example.util.LocalBodyFontFamily
import com.example.util.LocalHeadingFontFamily

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = NothingBlack,
    primaryContainer = NothingDarkSurfaceVariant,
    onPrimaryContainer = NothingDarkTextPrimary,
    secondary = NothingDarkTextSecondary,
    onSecondary = NothingDarkTextPrimary,
    secondaryContainer = NothingDarkSurfaceVariant,
    onSecondaryContainer = NothingDarkTextPrimary,
    tertiary = NothingSignalRed,
    onTertiary = Color.White,
    tertiaryContainer = NothingSignalRedContainer,
    onTertiaryContainer = NothingSignalRed,
    background = NothingBlack,
    onBackground = NothingDarkTextPrimary,
    surface = NothingDarkSurface,
    onSurface = NothingDarkTextPrimary,
    surfaceVariant = NothingDarkSurfaceVariant,
    onSurfaceVariant = NothingDarkTextSecondary,
    outline = NothingDarkBorder,
    outlineVariant = NothingDarkBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = NothingLightSurfaceVariant,
    onPrimaryContainer = NothingLightTextPrimary,
    secondary = NothingLightTextSecondary,
    onSecondary = NothingLightTextPrimary,
    secondaryContainer = NothingLightSurfaceVariant,
    onSecondaryContainer = NothingLightTextPrimary,
    tertiary = NothingSignalRed,
    onTertiary = Color.White,
    tertiaryContainer = NothingSignalRedContainer,
    onTertiaryContainer = NothingSignalRed,
    background = NothingLightBg,
    onBackground = NothingLightTextPrimary,
    surface = NothingLightSurface,
    onSurface = NothingLightTextPrimary,
    surfaceVariant = NothingLightSurfaceVariant,
    onSurfaceVariant = NothingLightTextSecondary,
    outline = NothingLightBorder,
    outlineVariant = NothingLightSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    accentColor: Color = LocalAccentColor.current,
    headingFont: FontFamily = LocalHeadingFontFamily.current,
    bodyFont: FontFamily = LocalBodyFontFamily.current,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicDark = DarkColorScheme.copy(
        tertiary = accentColor,
        tertiaryContainer = accentColor.copy(alpha = 0.15f),
        onTertiaryContainer = accentColor
    )
    val dynamicLight = LightColorScheme.copy(
        tertiary = accentColor,
        tertiaryContainer = accentColor.copy(alpha = 0.15f),
        onTertiaryContainer = accentColor
    )
    val colorScheme = if (darkTheme) dynamicDark else dynamicLight
    MaterialTheme(
        colorScheme = colorScheme,
        typography = createAppTypography(headingFont, bodyFont),
        content = content
    )
}
