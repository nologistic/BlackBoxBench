package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6F86C6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6DDF3),
    onPrimaryContainer = Color(0xFF002060),
    secondaryContainer = Color(0xFFF0EFF9),
    onSecondaryContainer = Color(0xFF001A4D),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF17171C),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF17171C),
    surfaceVariant = Color(0xFFF2ECF4),
    onSurfaceVariant = Color(0xFF5E6070),
    outline = Color(0xFFC7C5CA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF16802B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF103F1A),
    onPrimaryContainer = Color(0xFFF2FFF1),
    secondaryContainer = Color(0xFF102117),
    onSecondaryContainer = Color(0xFFF4FFF4),
    background = Color(0xFF141414),
    onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF444444)
)

@Composable
fun BenchmarkAppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
