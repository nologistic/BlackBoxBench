package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object Palette {
    val Accent = Color(0xFF1A73E8)
    val AccentDark = Color(0xFF1557B0)
    val Bg = Color(0xFFEDEDED)
    val BgDark = Color(0xFF1E1E1E)
    val SheetBg = Color(0xFFF7F7F7)
    val TextPrimary = Color(0xFF202124)
    val TextSecondary = Color(0xFF5F6368)
    val Scrim = Color(0x99000000)
    val Indicator = Color(0xFF7A7A7A)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    BenchmarkAppTheme {
        val context = LocalContext.current
        val state = remember { AppState(context) }
        MaterialTheme(colorScheme = lightColorScheme(primary = Palette.Accent)) {
            Box(modifier = Modifier.fillMaxSize().background(Palette.Bg)
                .windowInsetsPadding(WindowInsets.systemBars)) {
                when (state.screen) {
                    AppScreen.EDITOR -> EditorScreen(state)
                    AppScreen.VIEW_EDITS -> ViewEditsScreen(state)
                    AppScreen.INFO -> InfoScreen(state)
                    AppScreen.SETTINGS -> SettingsScreen(state)
                }
            }
        }
    }
}
