package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("calculator_state", 0) }
    var selectedTheme by remember {
        mutableStateOf(preferences.getString("theme", "系统默认") ?: "系统默认")
    }
    val dark = selectedTheme == "深色主题" ||
        selectedTheme == "深红色" ||
        (selectedTheme == "系统默认" && isSystemInDarkTheme())

    BenchmarkAppTheme(dark = dark) {
        val activity = context as ComponentActivity
        val barColor = MaterialTheme.colorScheme.background.toArgb()
        val lightBars = !dark
        SideEffect {
            activity.window.statusBarColor = barColor
            activity.window.navigationBarColor = barColor
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }

        var route by rememberSaveable { mutableStateOf("calculator") }
        var converterId by rememberSaveable { mutableStateOf("length") }

        BackHandler(route != "calculator") {
            route = when (route) {
                "converter" -> "categories"
                "appearance", "widget" -> "settings"
                else -> "calculator"
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (route) {
                "calculator" -> CalculatorScreen(
                    onOpenConverters = { route = "categories" },
                    onOpenSettings = { route = "settings" },
                    onOpenAbout = { route = "about" }
                )
                "categories" -> ConverterCategoriesScreen(
                    onBack = { route = "calculator" },
                    onCategory = {
                        converterId = it
                        route = "converter"
                    }
                )
                "converter" -> UnitConverterScreen(
                    categoryId = converterId,
                    onBack = { route = "categories" }
                )
                "settings" -> SettingsScreen(
                    onBack = { route = "calculator" },
                    onAppearance = { route = "appearance" },
                    onWidget = { route = "widget" }
                )
                "appearance" -> AppearanceScreen(
                    selectedTheme = selectedTheme,
                    onThemeSelected = {
                        selectedTheme = it
                        preferences.edit().putString("theme", it).apply()
                    },
                    onBack = { route = "settings" }
                )
                "widget" -> WidgetColorScreen(onBack = { route = "settings" })
                "about" -> AboutScreen(onBack = { route = "calculator" })
            }
        }
    }
}
