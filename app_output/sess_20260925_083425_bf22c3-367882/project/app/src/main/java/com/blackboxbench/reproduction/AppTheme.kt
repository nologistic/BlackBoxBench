package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark theme matching the observed target's palette. */
@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF2AA79B),
            onPrimary = Color(0xFF0B1D1A),
            background = Palette.Background,
            onBackground = Color(0xFFE4E7EF),
            surface = Palette.Background,
            onSurface = Color(0xFFE4E7EF),
            surfaceVariant = Color(0xFF1E2230),
            onSurfaceVariant = Color(0xFFC6CBDA),
            secondary = Color(0xFF2AA79B)
        ),
        content = content
    )
}
