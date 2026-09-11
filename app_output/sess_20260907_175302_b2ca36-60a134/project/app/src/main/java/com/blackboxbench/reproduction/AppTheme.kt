package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF18191F)
val CardBackground = Color(0xFF22232B)
val NavBackground = Color(0xFF292C39)
val PrimaryBlue = Color(0xFFA9BEFF)
val DeepBlue = Color(0xFF123268)
val PrimaryContainer = Color(0xFF3C4A70)
val TextPrimary = Color(0xFFE9E8F0)
val TextSecondary = Color(0xFFB9B8C2)
val Outline = Color(0xFF555660)
val Plum = Color(0xFF6A4167)

private val ClockColors = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = DeepBlue,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = TextPrimary,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = NavBackground,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    secondary = Color(0xFFD6A7D4)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClockColors, content = content)
}
