package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Neutral dark theme entry point. */
@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            onPrimary = Palette.TabTextSelected,
            background = Palette.Background,
            onBackground = Palette.TextPrimary,
            surface = Palette.Dialog,
            onSurface = Palette.TextPrimary,
            surfaceVariant = Palette.Panel,
            onSurfaceVariant = Palette.TextMuted,
            secondary = Palette.Accent,
            tertiary = Palette.LedAmber,
            scrim = Color(0xCC000000)
        ),
        content = content
    )
}
