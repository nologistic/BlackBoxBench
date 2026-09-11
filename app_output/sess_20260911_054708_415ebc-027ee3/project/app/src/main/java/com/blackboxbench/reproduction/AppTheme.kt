package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LibreraBlue = Color(0xFF3F51B5)
val LibreraBlueDark = Color(0xFF303F9F)
val LibreraTeal = Color(0xFF008F83)
val PermissionGreen = Color(0xFF22CB73)

private val LibreraColors = lightColorScheme(
    primary = LibreraBlue,
    secondary = LibreraTeal,
    background = Color(0xFFF7F7F7),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF666666),
    onSurface = Color(0xFF686868)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LibreraColors, content = content)
}
