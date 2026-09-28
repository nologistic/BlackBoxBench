package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BenchmarkAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MinesweeperScreen()
                }
            }
        }
    }
}

@Composable
fun MinesweeperScreen() {
    var customConfig by remember { mutableStateOf(presetConfig(Difficulty.CUSTOM)) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(Difficulty.EASY) }
    var game by remember { mutableStateOf(GameController(presetConfig(Difficulty.EASY))) }

    fun startGame(config: GameConfig) {
        game = GameController(config)
        selected = config.difficulty
    }

    LaunchedEffect(game) {
        while (true) {
            delay(1000L)
            game.clockTick(System.currentTimeMillis())
            game.advanceSecond()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .systemBarsPadding()
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentPanel(
                text = formatMineCount(game.remainingMines),
                modifier = Modifier.width(98.dp).height(56.dp)
            )
            Spacer(Modifier.weight(1f))
            FaceButton(status = game.status, onClick = { startGame(game.config) })
            Spacer(Modifier.weight(1f))
            SegmentPanel(
                text = formatClock(game),
                modifier = Modifier.width(118.dp).height(56.dp)
            )
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.entries.forEach { difficulty ->
                DifficultyTab(
                    difficulty = difficulty,
                    selected = selected == difficulty,
                    onClick = {
                        if (difficulty == Difficulty.CUSTOM) {
                            showCustomDialog = true
                        } else {
                            startGame(presetConfig(difficulty))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(48.dp), contentAlignment = Alignment.Center) {
            StatusBanner(game.status)
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                game = game,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Licenses",
                color = Palette.LicenseText,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickableNoRipple { showLicenses = true }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
    }

    if (showCustomDialog) {
        CustomGameDialog(
            initial = customConfig,
            onCancel = { showCustomDialog = false },
            onStart = { config ->
                customConfig = config.copy(difficulty = Difficulty.CUSTOM)
                showCustomDialog = false
                startGame(customConfig)
            }
        )
    }

    if (showLicenses) {
        LicensesDialog(onClose = { showLicenses = false })
    }
}

private fun formatMineCount(value: Int): String =
    if (value < 0) "-" + (-value).toString() else value.coerceAtMost(999).toString().padStart(3, '0')

private fun formatClock(game: GameController): String {
    val seconds = game.config.timeLimit?.let { game.secondsLeft } ?: game.elapsed
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
