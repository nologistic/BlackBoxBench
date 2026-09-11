package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GalleryColors = lightColorScheme(
    primary = Color(0xFF7E95CE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3F5),
    onPrimaryContainer = Color(0xFF142C5A),
    secondaryContainer = Color(0xFFE5E9F8),
    onSecondaryContainer = Color(0xFF102A58),
    background = Color(0xFFFFFAFF),
    onBackground = Color(0xFF17233D),
    surface = Color(0xFFFFFAFF),
    onSurface = Color(0xFF17233D),
    surfaceVariant = Color(0xFFE8E9F4),
    onSurfaceVariant = Color(0xFF49566F),
    outline = Color(0xFF78849D)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GalleryColors, content = content)
}
