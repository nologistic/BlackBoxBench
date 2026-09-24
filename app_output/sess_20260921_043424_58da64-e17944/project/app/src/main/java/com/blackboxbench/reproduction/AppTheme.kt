package com.blackboxbench.reproduction

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF00639B),
    surface = Color(0xFFFBF9F5),
    background = Color(0xFFFBF9F5)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA5C8FF),
    onPrimary = Color(0xFF00306B),
    primaryContainer = Color(0xFF1F405F),
    surface = Color(0xFF1A1C1E),
    background = Color(0xFF1A1C1E)
)

private val BlackScheme = darkColorScheme(
    primary = Color(0xFFA5C8FF),
    onPrimary = Color(0xFF00306B),
    surface = Color.Black,
    background = Color.Black
)

@Composable
fun BenchmarkAppTheme(theme: String = "system", content: @Composable () -> Unit) {
    val scheme = when (theme) {
        "light" -> LightScheme
        "dark" -> DarkScheme
        "black" -> BlackScheme
        else -> if (isSystemInDarkTheme()) DarkScheme else LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
