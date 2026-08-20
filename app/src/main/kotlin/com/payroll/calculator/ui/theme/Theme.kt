package com.payroll.calculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Токены, которых нет в стандартной Material3 ColorScheme (ведомость/чипы). */
data class ExtendedColors(
    val surfaceSunken: Color,
    val inkFaint: Color,
    val line: Color,
    val accentStrong: Color,
    val flag: Color,
    val flagSoft: Color,
)

private val LightExtendedColors = ExtendedColors(
    surfaceSunken = LightSurfaceSunken,
    inkFaint = LightInkFaint,
    line = LightLine,
    accentStrong = LightAccentStrong,
    flag = LightFlag,
    flagSoft = LightFlagSoft,
)

private val DarkExtendedColors = ExtendedColors(
    surfaceSunken = DarkSurfaceSunken,
    inkFaint = DarkInkFaint,
    line = DarkLine,
    accentStrong = DarkAccentStrong,
    flag = DarkFlag,
    flagSoft = DarkFlagSoft,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = LightAccentStrong,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    onSurfaceVariant = LightInkSoft,
    outline = LightLine,
    error = LightFlag,
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = DarkAccentStrong,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    onSurfaceVariant = DarkInkSoft,
    outline = DarkLine,
    error = DarkFlag,
)

@Composable
fun PayrollCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PayrollTypography,
            content = content,
        )
    }
}

/** Короткий доступ к расширенным токенам темы: `PayrollTheme.extendedColors`. */
object PayrollTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
