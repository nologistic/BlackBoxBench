package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TealScheme = lightColorScheme(
    primary = Color(0xFF009688),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF00796B),
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TealScheme, content = content)
}
