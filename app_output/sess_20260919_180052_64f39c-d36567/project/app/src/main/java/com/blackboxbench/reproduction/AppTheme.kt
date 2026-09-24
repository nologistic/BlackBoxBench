package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF55C8C4),
            background = Color(0xFF07131F),
            surface = Color(0xFF172236),
            onBackground = Color(0xFFE7EAF0),
            onSurface = Color(0xFFE7EAF0)
        ),
        content = content
    )
}
