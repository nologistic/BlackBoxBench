package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AntennaLight = lightColorScheme(
    primary = Color(0xFF0086C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7E8F7),
    onPrimaryContainer = Color(0xFF001F2A),
    background = Color(0xFFF8FAFD),
    surface = Color(0xFFF8FAFD),
    surfaceVariant = Color(0xFFE5EAF0),
    outline = Color(0xFF73777B)
)

private val AntennaDark = darkColorScheme(
    primary = Color(0xFF5CC8FF),
    primaryContainer = Color(0xFF123B58),
    background = Color(0xFF1D2428),
    surface = Color(0xFF1D2428),
    surfaceVariant = Color(0xFF30383E)
)

@Composable
fun BenchmarkAppTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) AntennaDark else AntennaLight, content = content)
}
