package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CashewColors = lightColorScheme(
    primary = Color(0xFF6F8ECB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FA),
    onPrimaryContainer = Color(0xFF08152E),
    secondary = Color(0xFF5F687F),
    background = Color(0xFFFAFAFF),
    surface = Color(0xFFFAFAFF),
    surfaceVariant = Color(0xFFE8ECFC),
    onBackground = Color(0xFF08152E),
    onSurface = Color(0xFF08152E),
    outline = Color(0xFF9EA3AF)
)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CashewColors, typography = Typography(), content = content)
}
