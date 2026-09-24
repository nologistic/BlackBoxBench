package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palette measured from the observed target app. */
object Palette {
    val Background = Color(0xFF0E1621)
    val Panel = Color(0xFF16222E)
    val PanelActive = Color(0xFF3ED9C4)
    val TabIdle = Color(0xFF16222E)
    val TabTitleIdle = Color(0xFF8FA3B5)
    val TabTitleActive = Color(0xFF0E1621)
    val SubIdle = Color(0xFF6C8090)
    val SubActive = Color(0xFF14453F)

    val TileTop = Color(0xFF2C3A47)
    val TileBottom = Color(0xFF22303C)
    val TileEdge = Color(0xFF35455A)
    val TileFlat = Color(0xFF1A2430)

    val LedRed = Color(0xFFFF3B30)
    val LedAmber = Color(0xFFFFA02D)

    val WinBand = Color(0xFF11331E)
    val WinText = Color(0xFF4CD97B)
    val LoseBand = Color(0xFF3A1116)
    val LoseText = Color(0xFFFF5A5A)

    val Flag = Color(0xFFFF8A2B)
    val FlagPole = Color(0xFFD8DEE4)
    val MineTile = Color(0xFF5A1E1E)
    val Fog = Color(0xFF6B4FA8)

    val TextMain = Color(0xFFE6EDF3)

    fun number(count: Int): Color = when (count) {
        1 -> Color(0xFF4C8DF6)
        2 -> Color(0xFF35D07F)
        3 -> Color(0xFFE5484D)
        4 -> Color(0xFFA855F7)
        5 -> Color(0xFFF59E0B)
        6 -> Color(0xFF14B8A6)
        7 -> Color(0xFFEAB308)
        else -> Color(0xFF94A3B8)
    }
}

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.PanelActive,
            background = Palette.Background,
            surface = Palette.Panel,
            onPrimary = Palette.TabTitleActive,
            onBackground = Palette.TextMain,
            onSurface = Palette.TextMain
        ),
        content = content
    )
}
