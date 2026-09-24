package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2196F3),
    secondary = Color(0xFF3F6F9E),
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F1FA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF90CAF9),
    background = Color(0xFF0D1215),
    surface = Color(0xFF111619),
    surfaceVariant = Color(0xFF292930)
)

@Composable
fun BenchmarkAppTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
