package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 目标应用配色：深蓝主色 + 白底（取自目标引导页实测像素）。 */
val BrandNavy = Color(0xFF36618E)
val BrandLogoBlue = Color(0xFF2196F3)
val BrandOutline = Color(0xFFBECEDE)
val BrandTextDark = Color(0xFF3C3C3C)

private val AppColorScheme = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2EF),
    onPrimaryContainer = Color(0xFF12304C),
    secondary = BrandNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E2EF),
    onSecondaryContainer = Color(0xFF12304C),
    background = Color.White,
    onBackground = Color(0xFF1B1B1B),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF5A6470),
    outline = BrandOutline,
    outlineVariant = Color(0xFFDCE3EC)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
