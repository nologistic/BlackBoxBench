package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForestLight = lightColorScheme(
    primary = Color(0xFF006C47),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F7E7),
    onPrimaryContainer = Color(0xFF002115),
    secondaryContainer = Color(0xFFE9F3ED),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F3F1),
    onBackground = Color(0xFF1A1C1B),
    onSurface = Color(0xFF1A1C1B)
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFF72D9AA),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF005136),
    onPrimaryContainer = Color(0xFF93F6C5),
    secondaryContainer = Color(0xFF24372F),
    background = Color(0xFF0F1110),
    surface = Color(0xFF0F1110),
    surfaceVariant = Color(0xFF292C2A),
    onBackground = Color(0xFFE2E3E1),
    onSurface = Color(0xFFE2E3E1)
)

@Composable
fun BenchmarkAppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) ForestDark else ForestLight,
        content = content
    )
}
