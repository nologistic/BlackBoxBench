package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark game theme matching the observed black background. */
@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0E0E12),
            surface = Color(0xFF1D1D24),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}
