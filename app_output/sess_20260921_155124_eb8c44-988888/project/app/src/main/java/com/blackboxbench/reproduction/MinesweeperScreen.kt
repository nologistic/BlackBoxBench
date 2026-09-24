package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

fun configFor(screen: Screen, custom: CustomConfig): GameConfig = when (screen) {
    Screen.EASY -> GameConfig(Screen.EASY, 9, 9, 10, fog = false, safeFirstTap = true, countdownSeconds = null)
    Screen.MEDIUM -> GameConfig(Screen.MEDIUM, 13, 13, 30, fog = false, safeFirstTap = true, countdownSeconds = 300)
    Screen.HARD -> GameConfig(Screen.HARD, 15, 15, 50, fog = true, safeFirstTap = true, countdownSeconds = 180)
    Screen.CUSTOM -> GameConfig(
        screen = Screen.CUSTOM,
        cols = custom.size,
        rows = custom.size,
        mines = custom.mines,
        fog = custom.fog,
        safeFirstTap = custom.safeFirstTap,
        countdownSeconds = null
    )
}

@Composable
fun MinesweeperScreen() {
    var screen by remember { mutableStateOf(Screen.EASY) }
    var custom by remember { mutableStateOf(CustomConfig()) }
    var game by remember { mutableStateOf(newGame(configFor(Screen.EASY, CustomConfig()))) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val latest = rememberUpdatedState(game)
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            val t = System.currentTimeMillis()
            now = t
            val current = latest.value
            if (current.phase == Phase.PLAYING) game = tick(current, t)
        }
    }

    fun restart(target: Screen, config: CustomConfig) {
        screen = target
        val t = System.currentTimeMillis()
        now = t
        game = newGame(configFor(target, config))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(horizontal = 11.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LedDisplay(text = counterText(game), color = Palette.LedRed, digitWidth = 21.dp, digitHeight = 38.dp)
            Spacer(Modifier.weight(1f))
            SmileyButton(
                face = when (game.phase) {
                    Phase.WON -> Face.COOL
                    Phase.LOST -> Face.DEAD
                    else -> Face.SMILE
                },
                size = 46.dp
            ) { restart(screen, custom) }
            Spacer(Modifier.weight(1f))
            LedDisplay(
                text = timerText(game, now),
                color = if (game.config.countdownSeconds != null) Palette.LedAmber else Palette.LedRed,
                digitWidth = 20.dp,
                digitHeight = 36.dp
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DifficultyTab("Easy", "10 \uD83D\uDCA3", screen == Screen.EASY, Modifier.weight(1f)) {
                restart(Screen.EASY, custom)
            }
            DifficultyTab("Medium", "\u23F1", screen == Screen.MEDIUM, Modifier.weight(1f)) {
                restart(Screen.MEDIUM, custom)
            }
            DifficultyTab("Hard", "\uD83D\uDC41\u26A1", screen == Screen.HARD, Modifier.weight(1f)) {
                restart(Screen.HARD, custom)
            }
            DifficultyTab("Custom", "\u2699", screen == Screen.CUSTOM, Modifier.weight(1f)) {
                showCustomDialog = true
            }
        }
        when (game.phase) {
            Phase.WON -> {
                Spacer(Modifier.height(8.dp))
                Band("\uD83C\uDF89 You Win! \uD83C\uDF89", Palette.WinBand, Palette.WinText)
            }
            Phase.LOST -> {
                Spacer(Modifier.height(8.dp))
                Band("\uD83D\uDCA5 Game Over \uD83D\uDCA5", Palette.LoseBand, Palette.LoseText)
            }
            else -> Unit
        }
        Spacer(Modifier.weight(1.15f))
        BoardView(
            state = game,
            now = now,
            onTap = { index ->
                val t = System.currentTimeMillis()
                now = t
                game = tapCell(game, index, t)
            },
            onLongPress = { index ->
                val t = System.currentTimeMillis()
                now = t
                game = longPressCell(game, index, t)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
        val licenseClick = rememberUpdatedState { showLicenses = true }
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(Unit) { detectTapGestures { licenseClick.value() } },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Licenses",
                color = Palette.TabTitleIdle,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(34.dp))
    }

    if (showCustomDialog) {
        CustomGameDialog(
            initial = custom,
            onCancel = { showCustomDialog = false },
            onStart = { updated ->
                custom = updated
                showCustomDialog = false
                restart(Screen.CUSTOM, updated)
            }
        )
    }
    if (showLicenses) {
        LicensesDialog(onClose = { showLicenses = false })
    }
}

@Composable
private fun DifficultyTab(
    title: String,
    sub: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Palette.PanelActive else Palette.TabIdle)
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            color = if (active) Palette.TabTitleActive else Palette.TabTitleIdle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(3.dp))
        Text(
            sub,
            color = if (active) Palette.SubActive else Palette.SubIdle,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun Band(text: String, background: Color, foreground: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = foreground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun counterText(game: GameState): String {
    val remaining = game.remainingMines
    return if (remaining < 0) {
        val digits = (-remaining).coerceAtMost(99).toString().padStart(2, '0')
        "-$digits"
    } else {
        remaining.coerceAtMost(999).toString().padStart(3, '0')
    }
}

private fun timerText(game: GameState, now: Long): String {
    val seconds = displaySeconds(game, now)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
