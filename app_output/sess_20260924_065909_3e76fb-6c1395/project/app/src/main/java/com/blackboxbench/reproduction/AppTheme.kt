package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Theme entry point; follows the reproduced app's appearance setting. */
@Composable
fun BenchmarkAppTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF8AB4F8),
            background = Color(0xFF121317),
            surface = Color(0xFF1C1D22)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2E6DB4),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
