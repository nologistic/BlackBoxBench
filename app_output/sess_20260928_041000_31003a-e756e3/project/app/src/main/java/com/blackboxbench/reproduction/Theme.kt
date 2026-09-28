package com.blackboxbench.reproduction

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppColors {
    val Primary = Color(0xFF1565C0)
    val Fab = Color(0xFF1E88E5)
    val Title = Color(0xFF1565C0)
    val Background = Color(0xFFFFFFFF)
    val SurfaceTint = Color(0xFFF3F0F7)
    val Banner = Color(0xFFE3E3E3)
    val BannerButton = Color(0xFF3D5A73)
    val Chip = Color(0xFFEDEDED)
    val OnSurface = Color(0xFF1B1B1B)
    val SecondaryText = Color(0xFF6B6B6B)
    val Divider = Color(0xFFE4E4E4)
    val Outline = Color(0xFF9E9E9E)
    val Green = Color(0xFF2E9E5B)
    val LowPriority = Color(0xFF2196F3)
    val MediumPriority = Color(0xFFF2B01E)
    val HighPriority = Color(0xFFE0362C)
    val NoPriority = Color(0xFF9E9E9E)
}

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Primary,
            onPrimary = Color.White,
            background = AppColors.Background,
            onBackground = AppColors.OnSurface,
            surface = AppColors.Background,
            onSurface = AppColors.OnSurface,
            surfaceVariant = AppColors.Chip,
            outline = AppColors.Outline,
            secondaryContainer = AppColors.Chip
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 16.sp),
            bodyMedium = TextStyle(fontSize = 14.sp),
            titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal)
        ),
        content = content
    )
}
