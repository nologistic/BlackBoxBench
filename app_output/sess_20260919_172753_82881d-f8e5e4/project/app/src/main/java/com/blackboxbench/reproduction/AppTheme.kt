package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Neutral theme entry point. It contains no target-specific visual choices. */
@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF131C2B),
            surface = Color(0xFF1B2635),
            primary = Color(0xFF7FA6D9)),
        content = content)
}
