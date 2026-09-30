package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("minesweeper", Context.MODE_PRIVATE)
        val initialDifficulty = Difficulty.fromName(prefs.getString("difficulty", null))
        setContent {
            BackHandler {}
            BenchmarkAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MinesweeperScreen(
                        initialDifficulty = initialDifficulty,
                        onDifficultyChanged = { d ->
                            prefs.edit().putString("difficulty", d.name).apply()
                        }
                    )
                }
            }
        }
    }
}
