package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClockColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF173057),
    secondary = Color(0xFFE3B6DE),
    onSecondary = Color(0xFF43203F),
    background = Color(0xFF1B1B22),
    onBackground = Color(0xFFE7E1EB),
    surface = Color(0xFF23242D),
    onSurface = Color(0xFFE7E1EB),
    surfaceVariant = Color(0xFF2B2D38),
    onSurfaceVariant = Color(0xFFC9C5CF),
    outline = Color(0xFF90909B)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClockColors, content = content)
}
