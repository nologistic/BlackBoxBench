package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VinylScheme = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFFFF006E),
    tertiary = Color(0xFFFF006E),
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF202020),
    onSurface = Color(0xFF202020)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VinylScheme, content = content)
}
