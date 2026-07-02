package com.guitartuna.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
val Background = Color(0xFF0D1117)
val Surface = Color(0xFF161B22)
val SurfaceLight = Color(0xFF21262D)
val TextPrimary = Color(0xFFEAEAEA)
val TextSecondary = Color(0xFF8B949E)
val Accent = Color(0xFF58A6FF)

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

@Composable
fun GuitarTunaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            window.navigationBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
