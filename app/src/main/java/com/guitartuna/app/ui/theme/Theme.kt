package com.guitartuna.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val TuneGreen = Color(0xFF00E676)
val TuneYellow = Color(0xFFFFD600)
val TuneOrange = Color(0xFFFF9100)
val TuneRed = Color(0xFFFF1744)

// Dark theme
val Background = Color(0xFF0D1117)
val Surface = Color(0xFF161B22)
val SurfaceLight = Color(0xFF21262D)
val TextPrimary = Color(0xFFEAEAEA)
val TextSecondary = Color(0xFF8B949E)
val Accent = Color(0xFF58A6FF)

// Light theme — high contrast for outdoor visibility
val LightBackground = Color(0xFFFAF8F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0EDE8)
val LightTextPrimary = Color(0xFF1A1A1A)
val LightTextSecondary = Color(0xFF5C5C5C)
val LightAccent = Color(0xFF1A73E8)

private val DarkColors = darkColorScheme(
    primary = TuneGreen,
    secondary = Accent,
    background = Background,
    surface = Surface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

private val LightColors = lightColorScheme(
    primary = TuneGreen,
    secondary = LightAccent,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
)

@Composable
fun GuitarTunaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
