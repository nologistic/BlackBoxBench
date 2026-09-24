package com.blackboxbench.reproduction

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Blue = Color(0xFF2196F3)
    val DarkBlue = Color(0xFF1976D2)
    val Red = Color(0xFFE53935)
    val Yellow = Color(0xFFFDD835)
    val Gray = Color(0xFF9E9E9E)
    val BottomBar = Color(0xFFF3EEFA)
    val BannerBg = Color(0xFFE9EBF5)
    val DrawerBg = Color(0xFFF7F7F7)
    val CardBg = Color.White
    val Subtle = Color(0xFF757575)

    /** 条目颜色选择器的预置色板 */
    val palette = listOf(
        0xD50000, 0xFF5252, 0xFF5722, 0xFF6D00, 0xFF9100,
        0xFFB300, 0xFFC107, 0xFFD600, 0xFFEA00, 0xFFEE58,
        0xEEFF41, 0xC6FF00, 0xAEEA00, 0x9CCC65, 0x8BC34A,
        0x66BB6A, 0x4CAF50, 0x009688, 0x00BFA5, 0x00B8D4,
        0x00B0FF, 0x03A9F4, 0x2196F3, 0x448AFF, 0x536DFE,
        0x3F51B5, 0x7986CB, 0xB39DDB, 0x9C7BB0, 0x7B1FA2,
        0x9C27B0, 0xAB47BC, 0xC2185B, 0xE91E63, 0xFF4081,
        0xFF8A80, 0x8D6E63, 0x616161, 0xBCAAA4, 0xBDBDBD,
        0x78909C, 0x000000
    ).map { it or 0xFF000000.toInt() }
}

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    val themePref = Store.setting("theme", "system")
    val dark = when (themePref) {
        "light" -> false
        "dark", "black" -> true
        else -> isSystemInDarkTheme()
    }
    val accent = Store.setting("accent", "").toLongOrNull()?.let { Color(it) } ?: AppColors.Blue
    val scheme = if (dark) darkColorScheme(primary = accent)
    else lightColorScheme(primary = accent, surface = Color.White, background = Color.White)
    MaterialTheme(colorScheme = scheme, content = content)
}
