package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BenchmarkAppTheme {
                val model = remember { AppModel() }
                when (model.screen) {
                    Screen.FILES -> FilesScreen(model)
                    Screen.SETTINGS -> {
                        BackHandler { model.screen = Screen.FILES }
                        SettingsScreen(model) { model.screen = Screen.FILES }
                    }
                    Screen.ABOUT -> {
                        BackHandler { model.screen = Screen.FILES }
                        AboutScreen { model.screen = Screen.FILES }
                    }
                    Screen.FTP -> {
                        BackHandler { model.screen = Screen.FILES }
                        FtpScreen(model) { model.screen = Screen.FILES }
                    }
                }
            }
        }
    }
}
