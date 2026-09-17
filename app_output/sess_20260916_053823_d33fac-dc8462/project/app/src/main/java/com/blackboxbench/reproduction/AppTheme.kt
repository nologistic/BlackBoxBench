package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1E88E5)
val DeepBlue = Color(0xFF3A5F8A)
val FabBlue = Color(0xFF2196F3)
val BannerGray = Color(0xFFE9EBF0)
val SectionText = Color(0xFF3C4043)

val PriorityNone = Color(0xFF9AA0A6)
val PriorityLow = Color(0xFF2196F3)
val PriorityMedium = Color(0xFFF5B301)
val PriorityHigh = Color(0xFFE53935)

fun priorityColor(p: Int) = when (p) {
    3 -> PriorityHigh
    2 -> PriorityMedium
    1 -> PriorityLow
    else -> PriorityNone
}

private val LightScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = DeepBlue,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF202124),
    surface = Color.White,
    onSurface = Color(0xFF202124),
    surfaceVariant = BannerGray,
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFDADCE0),
    error = PriorityHigh
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF10233F),
    secondary = Color(0xFF8AB4F8),
    onSecondary = Color(0xFF10233F),
    background = Color(0xFF1B1C1E),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF232427),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF2C2D31),
    onSurfaceVariant = Color(0xFFC6C7CA),
    outline = Color(0xFF44474E)
)

@Composable
fun BenchmarkAppTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}
