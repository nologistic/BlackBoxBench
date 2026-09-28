package com.blackboxbench.reproduction

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** AntennaPod-like palette. */
val AppBlue = Color(0xFF1A73E8)
val AppBlueMuted = Color(0xFF2E6DA4)
val AppBg = Color(0xFFE8F0FA)
val AppBgDark = Color(0xFF101418)
val AppSurfaceDark = Color(0xFF1B1F24)
val AppText = Color(0xFF1F1F1F)
val AppTextMuted = Color(0xFF4A4A4A)
val AppGrey = Color(0xFF5F6368)
val AppDivider = Color(0xFFCBD5E1)
val AppSwitchOff = Color(0xFFBFC8D4)

private val LightScheme = lightColorScheme(
    primary = AppBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF0B2A50),
    secondary = AppBlueMuted,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3FD),
    onSecondaryContainer = Color(0xFF0B2A50),
    tertiary = AppBlueMuted,
    background = AppBg,
    onBackground = AppText,
    surface = AppBg,
    onSurface = AppText,
    surfaceVariant = Color(0xFFEDF2FA),
    onSurfaceVariant = AppTextMuted,
    surfaceContainerHigh = Color.White,
    surfaceContainer = Color(0xFFF3F6FC),
    outline = Color(0xFF9AA5B1),
    outlineVariant = AppDivider,
    error = Color(0xFFB3261E),
    onError = Color.White,
    scrim = Color(0x99000000),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9CC7F5),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF11497E),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF9CC7F5),
    onSecondary = Color(0xFF00315C),
    background = Color(0xFF12161B),
    onBackground = Color(0xFFE2E6EC),
    surface = Color(0xFF12161B),
    onSurface = Color(0xFFE2E6EC),
    surfaceVariant = Color(0xFF262C33),
    onSurfaceVariant = Color(0xFFC3CAD3),
    surfaceContainerHigh = Color(0xFF1E2329),
    surfaceContainer = Color(0xFF1A1F25),
    outline = Color(0xFF8A929C),
    outlineVariant = Color(0xFF3A424B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

private val PureBlackScheme = DarkScheme.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainer = Color(0xFF0D0D0D),
)

/** Neutral theme entry point, parameterised by the persisted user preferences. */
@Composable
fun BenchmarkAppTheme(
    themeMode: String = "auto",
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val scheme = when {
        !dark -> LightScheme
        pureBlack -> PureBlackScheme
        else -> DarkScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
