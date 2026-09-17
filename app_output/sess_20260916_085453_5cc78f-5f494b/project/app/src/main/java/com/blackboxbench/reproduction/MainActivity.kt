package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = PageBg,
            surface = PageBg,
            onBackground = Ink,
            onSurface = Ink,
            primary = Color(0xFF415F91)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = PageBg) {
            val context = LocalContext.current
            val prefs = remember { Prefs(context) }
            val navigation = rememberNavController()
            NavHost(navController = navigation, startDestination = "calculator") {
                composable("calculator") {
                    CalculatorScreen(
                        prefs = prefs,
                        onOpenConverter = { navigation.navigate("converter") },
                        onOpenSettings = { navigation.navigate("settings") },
                        onOpenAbout = { navigation.navigate("about") }
                    )
                }
                composable("converter") {
                    ConverterHomeScreen(
                        onBack = { navigation.popBackStack() },
                        onOpen = { id -> navigation.navigate("converter/$id") }
                    )
                }
                composable(
                    "converter/{catId}",
                    arguments = listOf(navArgument("catId") { type = NavType.StringType })
                ) { entry ->
                    val catId = entry.arguments?.getString("catId") ?: "length"
                    ConverterCategoryScreen(
                        catId = catId,
                        prefs = prefs,
                        onBack = { navigation.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        prefs = prefs,
                        onBack = { navigation.popBackStack() },
                        onOpenAppearance = { navigation.navigate("appearance") }
                    )
                }
                composable("appearance") {
                    AppearanceScreen(prefs = prefs, onBack = { navigation.popBackStack() })
                }
                composable("about") {
                    AboutScreen(onBack = { navigation.popBackStack() })
                }
            }
        }
    }
}
