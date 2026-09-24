package com.blackboxbench.reproduction

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import java.util.Calendar

data class MarkorColors(
    val toolbar: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val editorBackground: Color,
    val onToolbar: Color,
    val navBackground: Color,
    val marker: Color,
)

val LightPalette = MarkorColors(
    toolbar = Color(0xFF2B2A33),
    accent = Color(0xFFE53935),
    background = Color(0xFFF2F2F2),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF7B7B7B),
    divider = Color(0xFFE2E2E2),
    editorBackground = Color(0xFFF4F4F4),
    onToolbar = Color(0xFFFFFFFF),
    navBackground = Color(0xFF2B2A33),
    marker = Color(0xFFE08A2E),
)

val DarkPalette = MarkorColors(
    toolbar = Color(0xFF26252D),
    accent = Color(0xFFE53935),
    background = Color(0xFF17171C),
    surface = Color(0xFF202027),
    textPrimary = Color(0xFFECECEC),
    textSecondary = Color(0xFF9A9AA2),
    divider = Color(0xFF34343C),
    editorBackground = Color(0xFF17171C),
    onToolbar = Color(0xFFF2F2F2),
    navBackground = Color(0xFF202027),
    marker = Color(0xFFE0A050),
)

val BlackPalette = MarkorColors(
    toolbar = Color(0xFF000000),
    accent = Color(0xFFE53935),
    background = Color(0xFF000000),
    surface = Color(0xFF0D0D0D),
    textPrimary = Color(0xFFF0F0F0),
    textSecondary = Color(0xFF9A9A9A),
    divider = Color(0xFF262626),
    editorBackground = Color(0xFF000000),
    onToolbar = Color(0xFFF0F0F0),
    navBackground = Color(0xFF0A0A0A),
    marker = Color(0xFFE0A050),
)

val LocalMarkorColors = staticCompositionLocalOf { LightPalette }

/** Resolves the stored theme name into a concrete palette. */
@Composable
fun MarkorTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) {
        "深色", "黑色" -> true
        "浅色" -> false
        "Auto" -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour < 9 || hour >= 17
        }

        else -> isSystemInDarkTheme()
    }
    val palette = when {
        theme == "黑色" -> BlackPalette
        dark -> DarkPalette
        else -> LightPalette
    }
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            background = palette.background,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            onBackground = palette.textPrimary,
            surfaceVariant = palette.divider,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            background = palette.background,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            onBackground = palette.textPrimary,
            surfaceVariant = palette.divider,
        )
    }
    CompositionLocalProvider(LocalMarkorColors provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
