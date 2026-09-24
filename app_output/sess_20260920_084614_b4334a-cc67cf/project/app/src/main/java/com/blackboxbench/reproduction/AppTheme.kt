package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NonogramColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color(0xFF101010),
    onBackground = Color.White,
    surface = Color(0xFF1D1D1D),
    onSurface = Color.White,
    outline = Color(0xFF3E3E3E),
    error = Color(0xFFFF454B)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NonogramColors, content = content)
}
