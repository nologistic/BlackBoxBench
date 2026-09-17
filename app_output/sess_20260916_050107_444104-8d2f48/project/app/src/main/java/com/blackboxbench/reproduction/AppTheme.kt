package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark theme tuned to the observed game surface colors. */
@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0A1119),
            surface = Color(0xFF0A1119),
            onBackground = Color(0xFFE6EDF3),
            onSurface = Color(0xFFE6EDF3),
            primary = Color(0xFF3ED9C4),
            onPrimary = Color(0xFF06231F)
        ),
        content = content
    )
}