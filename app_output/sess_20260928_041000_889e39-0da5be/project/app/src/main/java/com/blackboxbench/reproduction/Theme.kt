package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VinylColors {
    val Primary = Color(0xFF3F51B5)
    val PrimaryDark = Color(0xFF303F9F)
    val Accent = Color(0xFFFF4081)
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)
    val SecondaryText = Color(0xFF757575)
    val Divider = Color(0xFFE0E0E0)
    val OnboardingTop = Color(0xFF6C59A8)
    val OnboardingBottom = Color(0xFFC7B7E6)
}

@Composable
fun VinylTheme(primary: Color = VinylColors.Primary, accent: Color = VinylColors.Accent, content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary,
        onPrimaryContainer = Color.White,
        secondary = accent,
        onSecondary = Color.White,
        tertiary = accent,
        background = VinylColors.Background,
        onBackground = Color(0xFF212121),
        surface = VinylColors.Surface,
        onSurface = Color(0xFF212121),
        surfaceVariant = Color(0xFFF2F2F2),
        onSurfaceVariant = VinylColors.SecondaryText,
        outline = VinylColors.Divider
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
            bodyMedium = TextStyle(fontSize = 14.sp),
            titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)
        ),
        content = content
    )
}

private val AlbumPalette = listOf(
    Color(0xFF5E7A55),
    Color(0xFF7A5E55),
    Color(0xFF555F7A),
    Color(0xFF7A5568),
    Color(0xFF6B7A55),
    Color(0xFF556F7A)
)

/** Deterministic cover colour for an album title. */
fun albumColor(name: String): Color {
    if (name.isEmpty()) return AlbumPalette[0]
    var h = 0
    for (c in name) h = h * 31 + c.code
    val idx = ((h % AlbumPalette.size) + AlbumPalette.size) % AlbumPalette.size
    return AlbumPalette[idx]
}

/** Colour used as the now-playing background (a slightly darker cover tone). */
fun albumColorDark(name: String): Color {
    val c = albumColor(name)
    return Color(
        red = c.red * 0.82f,
        green = c.green * 0.82f,
        blue = c.blue * 0.82f,
        alpha = 1f
    )
}
