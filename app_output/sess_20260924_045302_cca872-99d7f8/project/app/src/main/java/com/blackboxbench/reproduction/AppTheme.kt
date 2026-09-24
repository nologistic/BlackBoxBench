package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinesweeperColors = darkColorScheme(
    primary = Color(0xFF4FD8C6),
    onPrimary = Color(0xFF06231F),
    secondary = Color(0xFF4FD8C6),
    onSecondary = Color(0xFF06231F),
    background = Color(0xFF0B1420),
    onBackground = Color(0xFFE6ECF2),
    surface = Color(0xFF16202C),
    onSurface = Color(0xFFE6ECF2),
    surfaceVariant = Color(0xFF1E2A38),
    onSurfaceVariant = Color(0xFFA9B4C0),
    outline = Color(0xFF33424F)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MinesweeperColors, content = content)
}
