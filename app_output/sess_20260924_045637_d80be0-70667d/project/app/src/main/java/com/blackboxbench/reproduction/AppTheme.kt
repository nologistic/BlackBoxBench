package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Palette of the reproduced player: indigo chrome, pink accent. */
data class ViColors(
    val appBar: Color,
    val appBarText: Color,
    val background: Color,
    val card: Color,
    val text: Color,
    val subtitle: Color,
    val divider: Color,
    val accent: Color,
    val primary: Color,
    val icon: Color,
    val nowPlaying: Color,
    val nowPlayingControls: Color,
    val queueCard: Color,
    val highlighted: Color,
    val dark: Boolean
)

val LightVi = ViColors(
    appBar = Color(0xFF4A5AC8),
    appBarText = Color(0xFFFFFFFF),
    background = Color(0xFFF3F3F7),
    card = Color(0xFFFFFFFF),
    text = Color(0xFF1B1B1F),
    subtitle = Color(0xFF6B6B74),
    divider = Color(0xFFE2E2E8),
    accent = Color(0xFFF50057),
    primary = Color(0xFF4A5AC8),
    icon = Color(0xFF5A5A64),
    nowPlaying = Color(0xFF232323),
    nowPlayingControls = Color(0xFFE6E6EC),
    queueCard = Color(0xFFFFFFFF),
    highlighted = Color(0x14000000),
    dark = false
)

val DarkVi = ViColors(
    appBar = Color(0xFF3E4EB4),
    appBarText = Color(0xFFFFFFFF),
    background = Color(0xFF1C1C1C),
    card = Color(0xFF2C2C2C),
    text = Color(0xFFEDEDED),
    subtitle = Color(0xFFA0A0A8),
    divider = Color(0xFF3A3A3A),
    accent = Color(0xFFF50057),
    primary = Color(0xFF7C8AF0),
    icon = Color(0xFFC8C8D0),
    nowPlaying = Color(0xFF141414),
    nowPlayingControls = Color(0xFF2A2A2A),
    queueCard = Color(0xFF2E2E2E),
    highlighted = Color(0x33FFFFFF),
    dark = true
)

val BlackVi = DarkVi.copy(
    appBar = Color(0xFF1F1F1F),
    background = Color(0xFF000000),
    card = Color(0xFF101010),
    divider = Color(0xFF262626),
    nowPlaying = Color(0xFF000000),
    nowPlayingControls = Color(0xFF1A1A1A),
    queueCard = Color(0xFF141414)
)

val LocalViColors = staticCompositionLocalOf { LightVi }

fun viColorsFor(mode: String): ViColors = when (mode) {
    "暗色" -> DarkVi
    "黑色（AMOLED）" -> BlackVi
    else -> LightVi
}

@Composable
fun BenchmarkAppTheme(mode: String = "浅色", content: @Composable () -> Unit) {
    val colors = viColorsFor(mode)
    CompositionLocalProvider(LocalViColors provides colors) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.primary,
                background = colors.background,
                surface = colors.card,
                onSurface = colors.text
            ),
            content = content
        )
    }
}
