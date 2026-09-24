package com.blackboxbench.reproduction

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF00897B)
private val TealDark = Color(0xFF00574D)
private val Accent = Color(0xFFE53935)

/** 浅色主题：teal 主色 + 红色强调（对应探索所见配色）。 */
@Composable
fun VinylColors(): ColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = TealDark,
    secondary = TealDark,
    onSecondary = Color.White,
    tertiary = Accent,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF546E7A),
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(colorScheme = VinylColors(), content = content)
}
