package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** VLC-flavoured palette. The target app is a light UI with an orange accent. */
data class VlcPalette(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val icon: Color,
    val accent: Color,
    val chip: Color,
    val isDark: Boolean,
)

val LightPalette = VlcPalette(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF141414),
    textSecondary = Color(0xFF757575),
    divider = Color(0xFFE4E4E4),
    icon = Color(0xFF444444),
    accent = Color(0xFFFF8800),
    chip = Color(0xFFF4F4F4),
    isDark = false,
)

val DarkPalette = VlcPalette(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    textPrimary = Color(0xFFEDEDED),
    textSecondary = Color(0xFF9E9E9E),
    divider = Color(0xFF2E2E2E),
    icon = Color(0xFFCFCFCF),
    accent = Color(0xFFFF8800),
    chip = Color(0xFF262626),
    isDark = true,
)

val LocalVlcPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun BenchmarkAppTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(
                primary = Color(0xFFFF8800),
                background = palette.background,
                surface = palette.surface,
                onSurface = palette.textPrimary,
                onBackground = palette.textPrimary,
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFFF8800),
                background = palette.background,
                surface = palette.surface,
                onSurface = palette.textPrimary,
                onBackground = palette.textPrimary,
            )
        },
        content = {
            CompositionLocalProvider(LocalVlcPalette provides palette, content = content)
        },
    )
}
