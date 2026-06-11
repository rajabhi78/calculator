package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = CyanAccent,
    tertiary = LightGray,
    background = DeepBlack,
    surface = KeypadBgDark,
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = GlassCardDark,
    outline = GlassBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = LitePrimary,
    secondary = LiteAccent,
    tertiary = LiteTextSecondary,
    background = LiteBackground,
    surface = KeypadBgLight,
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onBackground = LiteTextPrimary,
    onSurface = LiteTextPrimary,
    surfaceVariant = GlassCardLight,
    outline = GlassBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
