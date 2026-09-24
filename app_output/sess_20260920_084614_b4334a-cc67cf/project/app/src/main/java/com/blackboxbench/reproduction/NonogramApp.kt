package com.blackboxbench.reproduction

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.random.Random

@Composable
fun NonogramApp() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("nonogram_progress", Context.MODE_PRIVATE)
    }

    var page by remember { mutableStateOf(AppPage.HOME) }
    var currentLevel by remember { mutableIntStateOf(preferences.getInt("current_level", 1)) }
    var haptic by remember { mutableStateOf(preferences.getBoolean("haptic", true)) }
    var longPress by remember { mutableStateOf(preferences.getBoolean("long_press", true)) }
    var cycle by remember { mutableStateOf(preferences.getBoolean("cycle", false)) }
    var activePuzzle by remember { mutableStateOf(levelPuzzle(currentLevel)) }
    var gameOrigin by remember { mutableStateOf(GameOrigin.HOME) }
    var showDifficulty by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    var multiplayerDifficulty by remember { mutableStateOf(Difficulty.EASY) }
    var roomDigits by remember { mutableStateOf(Random.nextInt(100000, 999999).toString()) }
    var joinCode by remember { mutableStateOf("") }

    fun persistSettings() {
        preferences.edit()
            .putBoolean("haptic", haptic)
            .putBoolean("long_press", longPress)
            .putBoolean("cycle", cycle)
            .apply()
    }

    fun startPuzzle(puzzle: Puzzle, origin: GameOrigin) {
        activePuzzle = puzzle
        gameOrigin = origin
        page = AppPage.GAME
    }

    fun boardFor(puzzle: Puzzle): List<Int> =
        decodeBoard(preferences.getString("board_" + puzzle.id, null), puzzle.size)

    fun elapsedFor(puzzle: Puzzle): Int =
        preferences.getInt("elapsed_" + puzzle.id, 0)

    fun saveGame(puzzle: Puzzle, board: List<Int>, elapsed: Int) {
        preferences.edit()
            .putString("board_" + puzzle.id, encodeBoard(board))
            .putInt("elapsed_" + puzzle.id, elapsed)
            .apply()
    }

    fun leaveGame() {
        page = when (gameOrigin) {
            GameOrigin.SELECT_LEVEL -> AppPage.SELECT_LEVEL
            GameOrigin.MULTIPLAYER -> AppPage.MULTIPLAYER
            GameOrigin.RANDOM, GameOrigin.HOME -> AppPage.HOME
        }
    }

    fun markCompletedAndHome() {
        val level = activePuzzle.id.removePrefix("level_").toIntOrNull()
        if (level != null) {
            currentLevel = max(currentLevel, level + 1)
            preferences.edit()
                .putInt("current_level", currentLevel)
                .putBoolean("completed_$level", true)
                .remove("board_" + activePuzzle.id)
                .remove("elapsed_" + activePuzzle.id)
                .apply()
        }
        page = AppPage.HOME
    }

    fun goNext() {
        val level = activePuzzle.id.removePrefix("level_").toIntOrNull()
        if (level != null) {
            val next = level + 1
            currentLevel = max(currentLevel, next)
            preferences.edit()
                .putInt("current_level", currentLevel)
                .putBoolean("completed_$level", true)
                .remove("board_" + activePuzzle.id)
                .remove("elapsed_" + activePuzzle.id)
                .apply()
            startPuzzle(levelPuzzle(next), GameOrigin.SELECT_LEVEL)
        } else {
            page = AppPage.HOME
        }
    }

    BackHandler(enabled = page != AppPage.HOME || showDifficulty || showReset) {
        when {
            showReset -> showReset = false
            showDifficulty -> showDifficulty = false
            page == AppPage.GAME -> leaveGame()
            page == AppPage.SELECT_LEVEL || page == AppPage.HOW_TO ||
                page == AppPage.SETTINGS || page == AppPage.MULTIPLAYER -> page = AppPage.HOME
            else -> page = AppPage.HOME
        }
    }

    when (page) {
        AppPage.HOME -> HomeScreen(
            currentLevel = currentLevel,
            onPlay = { startPuzzle(levelPuzzle(currentLevel), GameOrigin.HOME) },
            onSelect = { page = AppPage.SELECT_LEVEL },
            onRandom = { showDifficulty = true },
            onMultiplayer = { page = AppPage.MULTIPLAYER },
            onHowTo = { page = AppPage.HOW_TO },
            onSettings = { page = AppPage.SETTINGS }
        )

        AppPage.SELECT_LEVEL -> SelectLevelScreen(
            currentLevel = currentLevel,
            onLevel = { level -> startPuzzle(levelPuzzle(level), GameOrigin.SELECT_LEVEL) },
            onBack = { page = AppPage.HOME }
        )

        AppPage.HOW_TO -> HowToScreen(onBack = { page = AppPage.HOME })

        AppPage.SETTINGS -> SettingsScreen(
            haptic = haptic,
            longPress = longPress,
            cycle = cycle,
            onHaptic = {
                haptic = it
                persistSettings()
            },
            onLongPress = {
                longPress = it
                persistSettings()
            },
            onCycle = {
                cycle = it
                persistSettings()
            },
            onReset = { showReset = true },
            onBack = { page = AppPage.HOME }
        )

        AppPage.MULTIPLAYER -> {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val roomCode = multiplayerDifficulty.title + "-" + roomDigits
            MultiplayerScreen(
                selected = multiplayerDifficulty,
                roomDigits = roomDigits,
                joinCode = joinCode,
                onDifficulty = { multiplayerDifficulty = it },
                onRefresh = { roomDigits = Random.nextInt(100000, 999999).toString() },
                onCopy = {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Room code", roomCode))
                },
                onPaste = {
                    joinCode = clipboard.primaryClip
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                        ?.uppercase()
                        .orEmpty()
                },
                onJoinCode = { joinCode = it },
                onStart = {
                    val seed = roomDigits.toLongOrNull() ?: 123456L
                    startPuzzle(
                        seededPuzzle(
                            multiplayerDifficulty,
                            seed,
                            id = "multi_" + multiplayerDifficulty.title + "_" + roomDigits
                        ),
                        GameOrigin.MULTIPLAYER
                    )
                },
                onJoin = {
                    val difficulty = Difficulty.fromCode(joinCode)
                    val digits = joinCode.substringAfter("-", "").filter { it.isDigit() }
                    if (difficulty != null && digits.length == 6) {
                        startPuzzle(
                            seededPuzzle(
                                difficulty,
                                digits.toLong(),
                                id = "multi_" + difficulty.title + "_" + digits
                            ),
                            GameOrigin.MULTIPLAYER
                        )
                    }
                },
                onBack = { page = AppPage.HOME }
            )
        }

        AppPage.GAME -> GameScreen(
            puzzle = activePuzzle,
            initialBoard = boardFor(activePuzzle),
            initialElapsed = elapsedFor(activePuzzle),
            cycleMode = cycle,
            longPressToCross = longPress,
            hapticEnabled = haptic,
            onPersist = { board, elapsed -> saveGame(activePuzzle, board, elapsed) },
            onBack = { leaveGame() },
            onNextLevel = { goNext() },
            onHome = { markCompletedAndHome() }
        )
    }

    if (showDifficulty) {
        DifficultyDialog(
            onChoose = { difficulty ->
                showDifficulty = false
                val seed = System.currentTimeMillis()
                startPuzzle(
                    seededPuzzle(difficulty, seed, id = "random_" + difficulty.title + "_" + seed),
                    GameOrigin.RANDOM
                )
            },
            onDismiss = { showDifficulty = false }
        )
    }

    if (showReset) {
        ResetProgressDialog(
            onConfirm = {
                preferences.edit().clear().apply()
                currentLevel = 1
                haptic = true
                longPress = true
                cycle = false
                activePuzzle = levelPuzzle(1)
                showReset = false
            },
            onDismiss = { showReset = false }
        )
    }
}
