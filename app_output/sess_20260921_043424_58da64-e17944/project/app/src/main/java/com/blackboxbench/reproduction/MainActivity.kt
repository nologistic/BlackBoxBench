package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store.get(applicationContext)
        setContent { ReproducedApp(store) }
    }
}

@Composable
fun ReproducedApp(store: Store) {
    store.version.value // subscribe recomposition to all store mutations
    BenchmarkAppTheme(theme = store.data.prefs.theme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navigation = rememberNavController()
            val start = if (store.data.prefs.setupDone) "main" else "welcome"
            AppNavHost(navigation, start, store)
        }
    }
}

@Composable
private fun AppNavHost(
    navigation: NavHostController,
    start: String,
    store: Store
) {
    NavHost(navController = navigation, startDestination = start) {
        composable("welcome") {
            WelcomeScreen(
                onContinue = {
                    store.completeSetup()
                    navigation.navigate("main") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onAddAccount = { navigation.navigate("accounts") }
            )
        }
        composable("accounts") {
            AccountsScreen(
                onBack = { navigation.popBackStack() },
                onOpenLogin = { navigation.navigate("cloudlogin") }
            )
        }
        composable("cloudlogin") {
            CloudLoginScreen(onBack = { navigation.popBackStack() })
        }
        composable("main") {
            MainScreen(
                store = store,
                onEditTask = { id ->
                    navigation.navigate("edit?taskId=" + (id ?: "-1"))
                },
                onOpenSettings = { navigation.navigate("settings") }
            )
        }
        composable(
            route = "edit?taskId={taskId}",
            arguments = listOf(navArgument("taskId") { defaultValue = "-1" })
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: "-1"
            EditScreen(store = store, taskId = taskId, onBack = { navigation.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                store = store,
                onBack = { navigation.popBackStack() },
                onOpenAppearance = { navigation.navigate("appearance") }
            )
        }
        composable("appearance") {
            AppearanceScreen(store = store, onBack = { navigation.popBackStack() })
        }
    }
}
