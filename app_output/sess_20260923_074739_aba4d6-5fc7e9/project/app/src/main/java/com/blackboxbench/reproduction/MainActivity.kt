package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = GameStorage(this)
        setContent {
            BenchmarkAppTheme {
                NonogramApp(store)
            }
        }
    }
}

@Composable
fun NonogramApp(store: GameStorage) {
    var screen by remember { mutableStateOf("game") }
    var state by remember { mutableStateOf(store.load()) }
    var epoch by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var showRestart by remember { mutableStateOf(false) }

    fun persist() = store.save(state)

    fun enterLevel(level: Int) {
        state = GameState(level = level, completed = state.completed)
        epoch++
        persist()
    }

    fun onCellTap(idx: Int) {
        if (state.won) return
        state = if (state.crossMode) {
            when {
                idx !in state.fills && idx !in state.marks ->
                    state.copy(marks = state.marks + idx, moves = state.moves + 1)
                idx in state.marks ->
                    state.copy(marks = state.marks - idx, moves = state.moves + 1)
                else -> state
            }
        } else {
            when {
                idx in state.fills -> state.copy(fills = state.fills - idx, moves = state.moves + 1)
                idx in state.marks -> state.copy(marks = state.marks - idx, moves = state.moves + 1)
                else -> state.copy(fills = state.fills + idx, moves = state.moves + 1)
            }
        }
        if (state.fills == Levels.solution(state.level)) {
            state = state.copy(won = true, completed = state.completed + state.level)
        }
        persist()
    }

    fun confirmRestart() {
        showRestart = false
        state = state.copy(
            fills = emptySet(), marks = emptySet(), moves = 0,
            crossMode = false, elapsed = 0, won = false
        )
        epoch++
        persist()
    }

    fun nextLevel() {
        if (state.level < Levels.TOTAL) {
            enterLevel(state.level + 1)
        } else {
            screen = "select"
        }
    }

    // 计时器：进入关卡/重启后归零计时；通关后停止；不持久化（重启 App 从零开始）
    LaunchedEffect(state.level, epoch) {
        while (true) {
            delay(1000)
            if (!state.won) state = state.copy(elapsed = state.elapsed + 1)
        }
    }

    // 红色横幅提示数秒后自动消失
    message?.let {
        LaunchedEffect(it) {
            delay(5000)
            message = null
        }
    }

    // 游戏界面吞掉系统返回键；关卡选择界面返回游戏
    if (screen == "game") {
        BackHandler { }
    } else {
        BackHandler { screen = "game" }
    }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        if (screen == "game") {
            GameScreen(
                state = state,
                message = message,
                onCellTap = ::onCellTap,
                onBack = { screen = "select" },
                onHint = { message = "No hints left" },
                onRestart = { showRestart = true },
                onModeSelect = { cross ->
                    state = state.copy(crossMode = cross)
                    persist()
                },
                onNextLevel = ::nextLevel
            )
        } else {
            SelectScreen(
                completed = state.completed,
                message = message,
                onBack = { screen = "game" },
                onPickLevel = { level ->
                    message = null
                    enterLevel(level)
                    screen = "game"
                },
                onLocked = { message = "Level locked!" }
            )
        }
        if (screen == "game" && state.won) {
            WinDialog(state = state, onNextLevel = ::nextLevel, lastLevel = state.level >= Levels.TOTAL)
        }
        if (screen == "game" && showRestart && !state.won) {
            RestartDialog(
                onCancel = { showRestart = false },
                onConfirm = ::confirmRestart
            )
        }
    }
}
