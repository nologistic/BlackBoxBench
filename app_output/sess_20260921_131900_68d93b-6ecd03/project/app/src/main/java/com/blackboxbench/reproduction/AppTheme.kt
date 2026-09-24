package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palette observed on the target app: near-black background, dark cards, white accents. */
object Palette {
    val Background = Color(0xFF111111)
    val Card = Color(0xFF212121)
    val CardHigh = Color(0xFF2A2A2A)
    val Tile = Color(0xFF232323)
    val Border = Color(0xFF3A3A3A)
    val BorderSoft = Color(0xFF2F2F2F)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9E9E9E)
    val TextMuted = Color(0xFF6B6B6B)
    val Red = Color(0xFFE8402A)
    val Amber = Color(0xFFF2B01E)
    val CellEmpty = Color(0xFF2B2B2B)
    val CellBorder = Color(0xFF3C3C3C)
}

object Metrics {
    val screenPadding = 24.dp
    val buttonHeight = 56.dp
    val radiusLarge = 14.dp
    val radiusSmall = 10.dp
    val titleLetterSpacing = 0.5.sp
}

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.White,
            onPrimary = Palette.Black,
            background = Palette.Background,
            onBackground = Palette.TextPrimary,
            surface = Palette.Background,
            onSurface = Palette.TextPrimary,
            surfaceVariant = Palette.Card,
            onSurfaceVariant = Palette.TextSecondary,
            outline = Palette.Border
        ),
        content = content
    )
}
