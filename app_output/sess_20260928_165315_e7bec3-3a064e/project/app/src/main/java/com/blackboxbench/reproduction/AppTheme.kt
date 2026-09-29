package com.blackboxbench.reproduction

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

val BlueColor = Color(APP_BLUE)

/** Observable current theme name, persisted via Settings.theme. */
object ThemeState {
    var current by mutableStateOf("system")
}

@Composable
fun BenchmarkAppTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark", "black" -> true
        "light", "day", "wallpaper" -> false
        else -> isSystemInDarkTheme().not() && false
    }
    val colors = if (dark) {
        if (theme == "black") {
            darkColorScheme(
                primary = BlueColor,
                onPrimary = Color.White,
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF161616),
                onBackground = Color(0xFFE8E8E8),
                onSurface = Color(0xFFE8E8E8),
                onSurfaceVariant = Color(0xFFB8B8B8)
            )
        } else {
            darkColorScheme(primary = BlueColor, onPrimary = Color.White)
        }
    } else {
        lightColorScheme(
            primary = BlueColor,
            onPrimary = Color.White,
            background = Color.White,
            surface = Color.White,
            surfaceVariant = Color(0xFFF5F5F8),
            onSurfaceVariant = Color(0xFF5A5A62)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
