package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NonogramScheme = darkColorScheme(
    primary = Color(0xFFF3F4F6),
    onPrimary = Color(0xFF101114),
    background = Color(0xFF0D0E11),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF1A1B20),
    onSurface = Color(0xFFF3F4F6)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NonogramScheme, content = content)
}
