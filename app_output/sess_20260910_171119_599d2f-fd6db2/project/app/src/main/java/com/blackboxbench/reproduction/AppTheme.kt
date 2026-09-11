package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnkiColors = lightColorScheme(
    primary = Color(0xFF079BE5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EFFF),
    onPrimaryContainer = Color(0xFF00344F),
    secondary = Color(0xFF006A8E),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    onSurface = Color(0xFF202124),
    outline = Color(0xFF74777A)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AnkiColors, content = content)
}
