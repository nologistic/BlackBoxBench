package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF6D7ED8)
val PrimaryDark = Color(0xFF12194A)
val DialogSurface = Color(0xFFF3EDF7)
val PillSurface = Color(0xFFE6E6EF)
val HintGrey = Color(0xFF6F6F78)
val DividerGrey = Color(0xFFE2E2E8)

private val Colors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Primary,
    onPrimaryContainer = Color.White,
    secondary = Primary,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF1B1B1B),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = PillSurface,
    onSurfaceVariant = HintGrey,
    surfaceContainerHigh = DialogSurface,
    outline = DividerGrey,
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
