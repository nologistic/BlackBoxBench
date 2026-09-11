package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VlcOrange = Color(0xFFFF5A00)
val VlcDark = Color(0xFF202124)
val SoftGray = Color(0xFFF3F3F3)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = VlcOrange,
            secondary = VlcOrange,
            background = Color.White,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = VlcDark,
            onSurface = VlcDark
        ),
        content = content
    )
}
