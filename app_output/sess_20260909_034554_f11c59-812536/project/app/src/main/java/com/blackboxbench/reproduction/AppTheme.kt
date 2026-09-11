package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFF5C6BC0),
    background = Color(0xFFF2F2F2),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8E8E8),
    onPrimary = Color.White,
    onBackground = Color(0xFF202020),
    onSurface = Color(0xFF202020)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF64B5F6),
    secondary = Color(0xFF90CAF9),
    background = Color(0xFF202020),
    surface = Color(0xFF303030),
    surfaceVariant = Color(0xFF3A3A3A),
    onPrimary = Color(0xFF101010),
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2)
)

@Composable
fun BenchmarkAppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
