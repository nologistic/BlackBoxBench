package com.blackboxbench.reproduction

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

private val ScreenBg = Color(0xFF0A0F1A)
private val LedGreen = Color(0xFF2EF06A)
private val SelectedBg = Color(0xFF1B8CA6)
private val UnselectedBg = Color(0xFF141E2C)
private val LicensesGray = Color(0xFF7A8697)
private const val INACTIVITY_PAUSE_MS = 30_000L

@Composable
fun MinesweeperScreen(
    initialDifficulty: Difficulty,
    onDifficultyChanged: (Difficulty) -> Unit
) {
    var difficulty by remember { mutableStateOf(initialDifficulty) }
    var game by remember { mutableStateOf(MinesweeperGame(difficulty)) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var lastInteractionMs by remember { mutableLongStateOf(0L) }
    var isResumed by remember { mutableStateOf(true) }
    var uiTick by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(game) {
        while (true) {
            delay(1000L)
            val active = game.status == GameStatus.PLAYING &&
                isResumed &&
                (lastInteractionMs == 0L ||
                    SystemClock.elapsedRealtime() - lastInteractionMs < INACTIVITY_PAUSE_MS)
            if (active) {
                if (elapsedSeconds < 99 * 60 + 59) elapsedSeconds++
                uiTick++
            }
        }
    }

    fun markInteraction() {
        lastInteractionMs = SystemClock.elapsedRealtime()
    }

    fun newGame(d: Difficulty) {
        difficulty = d
        onDifficultyChanged(d)
        game = MinesweeperGame(d)
        elapsedSeconds = 0
        lastInteractionMs = 0L
        uiTick++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedDisplay(text = formatMines(game.minesRemaining))
            LedDisplay(text = formatTime(elapsedSeconds))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.entries.forEach { d ->
                DifficultyChip(
                    label = d.label,
                    mineNote = "${d.mines} \uD83D\uDCA3",
                    selected = d == difficulty,
                    modifier = Modifier.weight(1f),
                    onClick = { newGame(d) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        BoardGrid(
            game = game,
            revision = uiTick,
            onCellTap = { r, c ->
                markInteraction()
                game.reveal(r, c)
                uiTick++
            },
            onCellLongPress = { r, c ->
                markInteraction()
                game.toggleFlag(r, c)
                uiTick++
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Licenses",
            color = LicensesGray,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 18.dp)
        )
    }
}

@Composable
private fun LedDisplay(text: String) {
    Text(
        text = text,
        color = LedGreen,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        letterSpacing = 2.sp
    )
}

@Composable
private fun DifficultyChip(
    label: String,
    mineNote: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) SelectedBg else UnselectedBg
    val titleColor = if (selected) Color(0xFF06222B) else Color(0xFFB9C6D6)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(0.5.dp, Color(0x26FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = mineNote,
                color = if (selected) Color(0xFF06303B) else Color(0xFF66768A),
                fontSize = 10.sp
            )
        }
    }
}

private fun formatMines(count: Int): String = String.format("%03d", count)

private fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
