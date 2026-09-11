package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF00833E),
    secondary = Color(0xFF2196F3),
    background = Color(0xFFF7F7F7),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF202124),
    onSurface = Color(0xFF202124)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF36C978),
    secondary = Color(0xFF64B5F6),
    background = Color(0xFF171918),
    surface = Color(0xFF232624),
    onPrimary = Color(0xFF082416),
    onBackground = Color(0xFFF1F3F2),
    onSurface = Color(0xFFF1F3F2)
)

@Composable
fun BenchmarkAppTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}
