package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ReproducedApp() }
    }
}

/** App-wide progress state, persisted in SharedPreferences like a real game. */
class GameProgress(context: Context) {
    private val prefs = context.getSharedPreferences("nonogram", Context.MODE_PRIVATE)

    var stars by mutableIntStateOf(prefs.getInt("stars", 3))
    var hearts by mutableIntStateOf(prefs.getInt("hearts", 5))
    var unlocked by mutableIntStateOf(prefs.getInt("unlocked", 1))
    var currentLevel by mutableIntStateOf(prefs.getInt("current", 1))
    var soundOn by mutableStateOf(prefs.getBoolean("sound", true))
    var hapticsOn by mutableStateOf(prefs.getBoolean("haptics", true))

    fun completeLevel(level: Int) {
        if (level >= unlocked && level < Puzzles.levelCount()) unlocked = level + 1
        currentLevel = if (level < Puzzles.levelCount()) level + 1 else level
        stars += 3
        hearts = 5
        save()
    }

    fun reset() {
        stars = 3; hearts = 5; unlocked = 1; currentLevel = 1
        save()
    }

    private fun save() {
        prefs.edit()
            .putInt("stars", stars)
            .putInt("hearts", hearts)
            .putInt("unlocked", unlocked)
            .putInt("current", currentLevel)
            .putBoolean("sound", soundOn)
            .putBoolean("haptics", hapticsOn)
            .apply()
    }
}

@Composable
fun ReproducedApp() {
    BenchmarkAppTheme {
        val nav = rememberNavController()
        val context = LocalContext.current
        val progress = remember { GameProgress(context) }

        NavHost(navController = nav, startDestination = "menu") {
            composable("menu") { MainMenuScreen(progress, nav) }
            composable("levels") { LevelSelectScreen(progress, nav) }
            composable("game/{level}") { entry ->
                val level = (entry.arguments?.getString("level") ?: "1").toInt()
                GameScreen(progress, nav, level, null)
            }
            composable("random") { GameScreen(progress, nav, 0, Puzzles.randomSolution()) }
            composable("howto") { HowToPlayScreen(nav) }
            composable("settings") { SettingsScreen(progress, nav) }
            composable("multiplayer") { MultiplayerScreen(nav) }
        }
    }
}
