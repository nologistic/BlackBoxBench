package com.blackboxbench.reproduction

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ClockBg = Color(0xFF121316)
val ClockSurface = Color(0xFF1E2026)
val ClockSurfaceHigh = Color(0xFF2A2D34)
val ClockAccent = Color(0xFFA8C7FA)
val ClockAccentDim = Color(0xFF3B4A6B)
val ClockText = Color(0xFFE6E8EC)
val ClockTextDim = Color(0xFF9BA1AA)
val ClockGroupHeader = Color(0xFF2E3138)

private val scheme = darkColorScheme(
    primary = ClockAccent,
    onPrimary = Color(0xFF0B1220),
    secondary = ClockAccent,
    background = ClockBg,
    onBackground = ClockText,
    surface = ClockSurface,
    onSurface = ClockText,
    surfaceVariant = ClockSurfaceHigh,
    onSurfaceVariant = ClockTextDim,
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}