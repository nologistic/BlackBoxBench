package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

sealed interface Screen {
    data object Home : Screen
    data object HowToPlay : Screen
    data object Settings : Screen
    data object SelectLevel : Screen
    data object Multiplayer : Screen

    enum class Kind { LEVEL, RANDOM, MULTIPLAYER }

    data class Game(
        val kind: Kind,
        val level: Int = 1,
        val difficulty: Difficulty = Difficulty.EASY,
        val code: String = ""
    ) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val appState = remember { AppState(context.applicationContext) }
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val current = stack.last()

    fun goTo(screen: Screen) {
        stack.add(screen)
    }

    fun replaceTop(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun goBack() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun goHome() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun startRandom(difficulty: Difficulty) {
        val seed = AppState.randomRoomCode()
        goTo(Screen.Game(Screen.Kind.RANDOM, difficulty = difficulty, code = seed))
    }

    BenchmarkAppTheme {
        Surface(color = Palette.Background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                when (current) {
                    is Screen.Home -> HomeScreen(
                        currentLevel = appState.currentLevel,
                        onPlay = {
                            goTo(Screen.Game(Screen.Kind.LEVEL, level = appState.currentLevel))
                        },
                        onSelectLevel = { goTo(Screen.SelectLevel) },
                        onRandom = { showDifficultyDialog = true },
                        onMultiplayer = { goTo(Screen.Multiplayer) },
                        onHowToPlay = { goTo(Screen.HowToPlay) },
                        onSettings = { goTo(Screen.Settings) }
                    )

                    is Screen.HowToPlay -> HowToPlayScreen(onBack = { goBack() })

                    is Screen.Settings -> SettingsScreen(
                        haptic = appState.hapticFeedback,
                        longPressToCross = appState.longPressToCross,
                        cycleMode = appState.cycleMode,
                        onHaptic = { appState.updateHaptic(it) },
                        onLongPress = { appState.updateLongPressToCross(it) },
                        onCycleMode = { appState.updateCycleMode(it) },
                        onResetProgress = { showResetDialog = true },
                        onBack = { goBack() }
                    )

                    is Screen.SelectLevel -> SelectLevelScreen(
                        completed = appState.completedLevels,
                        highestUnlocked = appState.highestUnlocked,
                        onBack = { goBack() },
                        onPick = { level ->
                            appState.updateCurrentLevel(level)
                            goTo(Screen.Game(Screen.Kind.LEVEL, level = level))
                        }
                    )

                    is Screen.Multiplayer -> MultiplayerScreen(
                        difficulty = appState.roomDifficulty,
                        roomDigits = appState.roomCode,
                        onDifficulty = { appState.updateRoomDifficulty(it) },
                        onRefreshCode = { appState.updateRoomCode(AppState.randomRoomCode()) },
                        onStart = { difficulty, code ->
                            goTo(Screen.Game(Screen.Kind.MULTIPLAYER, difficulty = difficulty, code = code))
                        },
                        onJoin = { difficulty, code ->
                            goTo(Screen.Game(Screen.Kind.MULTIPLAYER, difficulty = difficulty, code = code))
                        },
                        onBack = { goBack() }
                    )

                    is Screen.Game -> {
                        val game = current
                        val puzzle = when (game.kind) {
                            Screen.Kind.LEVEL -> Levels.puzzle(game.level)
                            Screen.Kind.RANDOM -> PuzzleGenerator.generate(game.difficulty, game.code)
                            Screen.Kind.MULTIPLAYER -> PuzzleGenerator.generate(game.difficulty, game.code)
                        }
                        val boardKey = when (game.kind) {
                            Screen.Kind.LEVEL -> BoardKeys.level(game.level)
                            Screen.Kind.RANDOM -> BoardKeys.random(game.difficulty, game.code)
                            Screen.Kind.MULTIPLAYER -> BoardKeys.multiplayer(game.code)
                        }
                        val title = if (game.kind == Screen.Kind.LEVEL) "LEVEL ${game.level}" else ""
                        val nextLevel = if (game.kind == Screen.Kind.LEVEL && game.level < Levels.count) {
                            game.level + 1
                        } else {
                            null
                        }
                        GameScreen(
                            puzzle = puzzle,
                            title = title,
                            boardKey = boardKey,
                            appState = appState,
                            nextLevel = nextLevel,
                            onBack = { goBack() },
                            onWin = { _, _ ->
                                if (game.kind == Screen.Kind.LEVEL) {
                                    appState.markCompleted(game.level)
                                    appState.updateCurrentLevel(
                                        (game.level + 1).coerceAtMost(Levels.count)
                                    )
                                }
                            },
                            onOpenLevel = { level ->
                                appState.updateCurrentLevel(level)
                                replaceTop(Screen.Game(Screen.Kind.LEVEL, level = level))
                            },
                            onHome = { goHome() }
                        )
                    }
                }

                if (showResetDialog) {
                    ConfirmResetDialog(
                        onConfirm = {
                            appState.resetProgress()
                            showResetDialog = false
                        },
                        onCancel = { showResetDialog = false }
                    )
                }

                if (showDifficultyDialog) {
                    DifficultyDialog(
                        onPick = { difficulty ->
                            showDifficultyDialog = false
                            startRandom(difficulty)
                        },
                        onCancel = { showDifficultyDialog = false }
                    )
                }
            }
        }
    }

    BackHandler(enabled = stack.size > 1 || showResetDialog || showDifficultyDialog) {
        when {
            showResetDialog -> showResetDialog = false
            showDifficultyDialog -> showDifficultyDialog = false
            else -> goBack()
        }
    }
}
