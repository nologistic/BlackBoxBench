package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

sealed class Screen {
    data object Onboarding : Screen()
    data object Home : Screen()
    data class Editor(val fileName: String) : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = EditorBg) {
                    MarkorRoot()
                }
            }
        }
    }
}

@Composable
fun MarkorRoot() {
    var screen by remember {
        mutableStateOf<Screen>(if (Store.onboarded) Screen.Home else Screen.Onboarding)
    }
    when (val s = screen) {
        is Screen.Onboarding -> OnboardingScreen(onDone = {
            Store.markOnboarded()
            screen = Screen.Home
        })
        is Screen.Home -> HomeScreen(
            onOpenFile = { screen = Screen.Editor(it) },
            onOpenSettings = { screen = Screen.Settings },
        )
        is Screen.Editor -> EditorScreen(
            fileName = s.fileName,
            onBack = { screen = Screen.Home },
        )
        is Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home })
    }
}
