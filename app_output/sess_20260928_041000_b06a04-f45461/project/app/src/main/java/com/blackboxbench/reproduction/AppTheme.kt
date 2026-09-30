package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palette matching the observed gallery app. */
object Palette {
    val toolbar = Color(0xFFE7E0EC)
    val searchPill = Color(0xFFD9D3E0)
    val onSurface = Color(0xFF1C1B1F)
    val onSurfaceVariant = Color(0xFF49454F)
    val accent = Color(0xFF6750A4)
    val accentDark = Color(0xFF5A4A8F)
    val background = Color(0xFFFFFFFF)
    val tileScrim = Color(0x99000000)
    val divider = Color(0xFFCAC4D0)
    val editorAccent = Color(0xFF3B5BDB)
    val star = Color(0xFFFFC107)
}

private val scheme = lightColorScheme(
    primary = Palette.accent,
    onPrimary = Color.White,
    surface = Color.White,
    onSurface = Palette.onSurface,
    surfaceVariant = Palette.toolbar,
    onSurfaceVariant = Palette.onSurfaceVariant,
    background = Palette.background,
    onBackground = Palette.onSurface,
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
