package com.blackboxbench.reproduction

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Palette
// ---------------------------------------------------------------------------
val BgColor = Color(0xFF0D0E11)
val CardColor = Color(0xFF1A1B20)
val CardColorDeep = Color(0xFF101114)
val BorderColor = Color(0xFF2C2D34)
val BorderBright = Color(0xFF4A4B54)
val TextMain = Color(0xFFF3F4F6)
val TextDim = Color(0xFF9AA0A8)
val TextFaint = Color(0xFF5F646C)
val CrossColor = Color(0xFFE5484D)
val FillColor = Color(0xFFF3F4F6)
val SelectPink = Color(0xFFEFC3CA)

private val Condensed = FontFamily.SansSerif

@Composable
fun AppRoot(state: AppState) {
    BackHandler(enabled = state.screen !is Screen.Menu) {
        state.screen = when (val current = state.screen) {
            is Screen.Play -> if (current.levelIndex != null) Screen.LevelSelect else Screen.Menu
            else -> Screen.Menu
        }
    }
    Box(Modifier.fillMaxSize().background(BgColor)) {
        when (val screen = state.screen) {
            is Screen.Menu -> MenuScreen(state)
            is Screen.LevelSelect -> LevelSelectScreen(state)
            is Screen.HowToPlay -> HowToPlayScreen(state)
            is Screen.Settings -> SettingsScreen(state)
            is Screen.Multiplayer -> MultiplayerScreen(state)
            is Screen.Play -> PuzzleScreen(
                state = state,
                puzzle = screen.puzzle,
                title = screen.title,
                levelIndex = screen.levelIndex,
                onBack = {
                    state.screen = if (screen.levelIndex != null) Screen.LevelSelect else Screen.Menu
                }
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

// ---------------------------------------------------------------------------
// Common pieces
// ---------------------------------------------------------------------------

@Composable
private fun TopBar(title: String, onBack: () -> Unit, centered: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 45.dp)
            .height(56.dp)
            .padding(horizontal = 18.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(34.dp)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            IconBack(TextMain)
        }
        Text(
            text = title,
            color = TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Condensed,
            letterSpacing = 0.6.sp,
            modifier = Modifier
                .align(if (centered) Alignment.Center else Alignment.CenterStart)
                .padding(start = if (centered) 0.dp else 44.dp)
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    fontSize: Int = 13,
    leading: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) CardColorDeep else Color.Transparent)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = TextMain,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Condensed,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MenuChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = TextMain,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Condensed,
            letterSpacing = 0.8.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Main menu
// ---------------------------------------------------------------------------

@Composable
private fun MenuScreen(state: AppState) {
    val context = LocalContext.current
    var showDifficulty by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 25.dp)
    ) {
        Spacer(Modifier.height(63.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable { openUrl(context, "https://example.com") },
                contentAlignment = Alignment.Center
            ) { IconStar(Color(0xFFFFC93C)) }
            MenuChip("LEVEL ${state.maxUnlocked}")
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable { openUrl(context, "https://example.com") },
                contentAlignment = Alignment.Center
            ) { IconHeart(Color(0xFFE5484D)) }
        }
        Spacer(Modifier.height(114.dp))
        Text(
            text = "NONOGRAM",
            color = TextMain,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            fontFamily = Condensed,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "PICTURE LOGIC PUZZLE",
            color = TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Condensed,
            letterSpacing = 3.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(103.dp))
        PillButton("PLAY", {
            val level = state.maxUnlocked.coerceIn(1, Levels.count)
            state.screen = Screen.Play(Levels.puzzles[level - 1], "LEVEL $level", level)
        }, Modifier.fillMaxWidth().height(44.dp), filled = true, fontSize = 14)
        Spacer(Modifier.height(22.dp))
        PillButton("SELECT LEVEL", { state.screen = Screen.LevelSelect }, Modifier.fillMaxWidth().height(44.dp))
        Spacer(Modifier.height(22.dp))
        PillButton("RANDOM PUZZLE", { showDifficulty = true }, Modifier.fillMaxWidth().height(44.dp))
        Spacer(Modifier.height(22.dp))
        PillButton("MULTIPLAYER", { state.screen = Screen.Multiplayer }, Modifier.fillMaxWidth().height(44.dp))
        Spacer(Modifier.height(22.dp))
        PillButton("HOW TO PLAY", { state.screen = Screen.HowToPlay }, Modifier.fillMaxWidth().height(44.dp))
        Spacer(Modifier.height(22.dp))
        PillButton("SETTINGS", { state.screen = Screen.Settings }, Modifier.fillMaxWidth().height(44.dp))
    }
    if (showDifficulty) {
        DifficultyDialog(
            onPick = { d ->
                showDifficulty = false
                state.screen = Screen.Play(
                    generatePuzzle(d.rows, d.cols, System.nanoTime()),
                    "RANDOM PUZZLE",
                    null
                )
            },
            onDismiss = { showDifficulty = false }
        )
    }
}

// ---------------------------------------------------------------------------
// Level select
// ---------------------------------------------------------------------------

@Composable
private fun LevelSelectScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("SELECT LEVEL", { state.screen = Screen.Menu })
        Spacer(Modifier.height(40.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 38.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            for (row in 0 until 3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    for (col in 0 until 4) {
                        val index = row * 4 + col
                        if (index < Levels.count) {
                            val level = index + 1
                            LevelTile(
                                level = level,
                                locked = level > state.maxUnlocked,
                                completed = state.completed.contains(level),
                                onClick = {
                                    if (level <= state.maxUnlocked) {
                                        state.screen = Screen.Play(Levels.puzzles[index], "LEVEL $level", level)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelTile(level: Int, locked: Boolean, completed: Boolean, onClick: () -> Unit) {
    val border = when {
        completed -> TextMain
        locked -> BorderColor
        else -> BorderBright
    }
    Box(
        Modifier
            .width(54.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardColor)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (locked) {
            IconLock(TextFaint)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$level",
                    color = TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Condensed
                )
                if (completed) {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.size(5.dp).clip(CircleShape).background(TextMain))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// How to play
// ---------------------------------------------------------------------------

@Composable
private fun HowToPlayScreen(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar("HOW TO PLAY", { state.screen = Screen.Menu })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 25.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(36.dp))
            HowCard(
                "REVEAL THE PICTURE",
                "NONOGRAMS ARE LOGIC PUZZLES WHERE GRID CELLS MUST BE FILLED OR LEFT BLANK ACCORDING TO NUMBERS AT THE SIDE OF THE GRID."
            )
            HowCard(
                "READ THE CLUES",
                "THE NUMBERS SHOW SEQUENCES OF FILLED CELLS IN THAT ROW OR COLUMN. E.G., '3 1' MEANS A BLOCK OF 3 FILLED CELLS FOLLOWED BY 1 FILLED CELL."
            )
            HowCard(
                "MARK BLANK SPACES",
                "TAP A CELL TO FILL IT, OR MARK EMPTY SPACES WITH AN 'X' TO KEEP TRACK OF SPACES THAT CANNOT BE FILLED."
            )
            HowCard(
                "COMPLETE THE GRID",
                "SOLVE THE ENTIRE PUZZLE USING LOGIC WITHOUT GUESSING TO REVEAL THE HIDDEN PIXEL IMAGE!"
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HowCard(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardColor)
            .padding(20.dp)
    ) {
        Text(
            text = title,
            color = TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Condensed,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            color = TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Condensed,
            letterSpacing = 0.4.sp,
            lineHeight = 17.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

@Composable
private fun SettingsScreen(state: AppState) {
    var showReset by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopBar("SETTINGS", { state.screen = Screen.Menu })
        Spacer(Modifier.height(32.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CardColor)
        ) {
            SettingRow("HAPTIC FEEDBACK", state.haptic) { state.updateHaptic(it) }
            SettingRow("LONG PRESS TO CROSS", state.longPressCross) { state.updateLongPressCross(it) }
            SettingRow("CYCLE MODE", state.cycleMode) { state.updateCycleMode(it) }
        }
        Spacer(Modifier.height(8.dp))
        PillButton("RESET PROGRESS", { showReset = true }, Modifier.fillMaxWidth().padding(horizontal = 25.dp))
    }
    if (showReset) {
        Overlay(onDismiss = { showReset = false }) {
            Column(
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(CardColor)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconWarning()
                Spacer(Modifier.height(12.dp))
                Text(
                    "RESET PROGRESS?",
                    color = TextMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Condensed,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "ARE YOU SURE YOU WANT TO RESET ALL YOUR GAME PROGRESS?\nTHIS ACTION CANNOT BE UNDONE.",
                    color = TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Condensed,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(20.dp))
                PillButton("RESET PROGRESS", {
                    state.resetProgress()
                    showReset = false
                    toast = true
                }, Modifier.fillMaxWidth().height(48.dp), filled = true)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).clickable { showReset = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("CANCEL", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed)
                }
            }
        }
    }
    if (toast) {
        LaunchedEffect(Unit) {
            delay(2200)
            toast = false
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .padding(bottom = 40.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE9EAEC))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    "ALL PROGRESS HAS BEEN RESET.",
                    color = Color(0xFF1A1B20),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Condensed
                )
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Condensed,
            letterSpacing = 0.6.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF1A1B20),
                checkedTrackColor = TextMain,
                checkedBorderColor = TextMain,
                uncheckedThumbColor = Color(0xFF6B7079),
                uncheckedTrackColor = Color(0xFF2A2B31),
                uncheckedBorderColor = Color(0xFF3A3B42)
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Multiplayer
// ---------------------------------------------------------------------------

@Composable
private fun MultiplayerScreen(state: AppState) {
    val context = LocalContext.current
    var difficulty by remember { mutableStateOf("EASY") }
    var digits by remember { mutableStateOf(String.format("%06d", Random.nextInt(0, 1000000))) }
    var joinText by remember { mutableStateOf("") }
    val code = "$difficulty-$digits"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopBar("MULTIPLAYER", { state.screen = Screen.Menu })
        Column(Modifier.fillMaxWidth().padding(horizontal = 25.dp)) {
            Spacer(Modifier.height(14.dp))
            Text("DIFFICULTY", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(66.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (d in DIFFICULTIES.take(4)) {
                    DifficultyChip(d.label, difficulty == d.key, Modifier.weight(1f)) { difficulty = d.key }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth()) {
                DifficultyChip(DIFFICULTIES[4].label, difficulty == DIFFICULTIES[4].key, Modifier.weight(1f)) { difficulty = DIFFICULTIES[4].key }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(58.dp))
            Text("YOUR ROOM CODE", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(29.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(code, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed, letterSpacing = 1.sp)
                }
                Spacer(Modifier.width(12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("room", code))
                    }.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCopy(TextMain)
                    Spacer(Modifier.width(7.dp))
                    Text("COPY CODE", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed)
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.size(38.dp).clip(CircleShape).clickable {
                        digits = String.format("%06d", Random.nextInt(0, 1000000))
                    },
                    contentAlignment = Alignment.Center
                ) { IconRefresh(TextMain) }
            }
            Spacer(Modifier.height(67.dp))
            PillButton("START PUZZLE", {
                val d = difficultyFor(difficulty)
                val seed = ("$difficulty-$digits").hashCode().toLong()
                state.screen = Screen.Play(generatePuzzle(d.rows, d.cols, seed), "MULTIPLAYER", null)
            }, Modifier.fillMaxWidth(), filled = true)
            Spacer(Modifier.height(40.dp))
            Text("JOIN WITH CODE", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(15.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (joinText.isEmpty()) {
                            Text("E.G. EASY-123456", color = TextFaint, fontSize = 13.sp, fontFamily = Condensed)
                        }
                        BasicTextField(
                            value = joinText,
                            onValueChange = { joinText = it.uppercase() },
                            singleLine = true,
                            textStyle = TextStyle(color = TextMain, fontSize = 13.sp, fontFamily = Condensed, fontWeight = FontWeight.Bold),
                            cursorBrush = SolidColor(TextMain),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                joinText = clip.getItemAt(0).coerceToText(context).toString().uppercase()
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) { IconPaste(TextDim) }
                }
            }
            Spacer(Modifier.height(41.dp))
            PillButton("JOIN PUZZLE", {
                val match = Regex("^([A-Z]+)-(\\d{6})$").find(joinText.trim())
                if (match != null) {
                    val key = match.groupValues[1]
                    val d = DIFFICULTIES.firstOrNull { it.key == key }
                    if (d != null) {
                        val seed = joinText.trim().hashCode().toLong()
                        state.screen = Screen.Play(generatePuzzle(d.rows, d.cols, seed), "MULTIPLAYER", null)
                    }
                }
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun DifficultyChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CardColorDeep else Color.Transparent)
            .border(1.dp, if (selected) TextMain else BorderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                IconCheck(TextMain)
                Spacer(Modifier.width(4.dp))
            }
            Text(label, color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed)
        }
    }
}

// ---------------------------------------------------------------------------
// Puzzle screen
// ---------------------------------------------------------------------------

@Composable
private fun PuzzleScreen(
    state: AppState,
    puzzle: Puzzle,
    title: String,
    levelIndex: Int?,
    onBack: () -> Unit
) {
    val total = puzzle.rows * puzzle.cols
    val marks = remember(puzzle) { mutableStateListOf<Int>().apply { addAll(puzzle.initialMarks.toList()) } }
    val history = remember(puzzle) { mutableStateListOf<Triple<Int, Int, Int>>() }
    var moves by remember(puzzle) { mutableIntStateOf(puzzle.given.size) }
    var seconds by remember(puzzle) { mutableIntStateOf(0) }
    var mode by remember(puzzle) { mutableIntStateOf(1) }
    var showComplete by remember(puzzle) { mutableStateOf(false) }

    LaunchedEffect(puzzle, showComplete) {
        if (!showComplete) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    fun isSolved(): Boolean {
        for (i in 0 until total) if ((marks[i] == 1) != puzzle.solutionAt(i)) return false
        return true
    }

    fun change(index: Int, next: Int) {
        if (showComplete) return
        val prev = marks[index]
        if (prev == next) return
        history.add(Triple(index, prev, moves))
        marks[index] = next
        if (next == 1 && prev != 1) moves++
        if (isSolved()) {
            showComplete = true
            if (levelIndex != null) state.completeLevel(levelIndex)
        }
    }

    fun handleTap(index: Int) {
        if (state.cycleMode) {
            change(index, (marks[index] + 1) % 3)
        } else if (mode == 2) {
            change(index, if (marks[index] == 2) 0 else 2)
        } else {
            change(index, if (marks[index] == 1) 0 else 1)
        }
    }

    fun handleLong(index: Int) {
        if (state.longPressCross) {
            change(index, if (marks[index] == 2) 0 else 2)
        } else {
            handleTap(index)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(title, onBack, centered = true)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconClock(TextMain)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "%02d:%02d".format(seconds / 60, seconds % 60),
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Condensed
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconMoves(TextMain)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "MOVES: $moves",
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Condensed
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton(
                    "UNDO",
                    {
                        val last = history.removeLastOrNull()
                        if (last != null) {
                            marks[last.first] = last.second
                            moves = last.third
                        }
                    },
                    Modifier.weight(1f),
                    fontSize = 12,
                    leading = { IconUndo(if (history.isEmpty()) TextFaint else TextMain) }
                )
                PillButton(
                    "HINT",
                    {
                        if (!showComplete) {
                            for (i in 0 until total) {
                                if (puzzle.solutionAt(i) && marks[i] != 1) {
                                    history.add(Triple(i, marks[i], moves))
                                    marks[i] = 1
                                    moves++
                                    if (isSolved()) {
                                        showComplete = true
                                        if (levelIndex != null) state.completeLevel(levelIndex)
                                    }
                                    break
                                }
                            }
                        }
                    },
                    Modifier.weight(1f),
                    fontSize = 12,
                    leading = { IconBulb(Color(0xFFFFB020)) }
                )
                PillButton(
                    "RESTART",
                    {
                        for (i in 0 until total) marks[i] = puzzle.initialMarks[i]
                        history.clear()
                        moves = puzzle.given.size
                        seconds = 0
                        showComplete = false
                    },
                    Modifier.weight(1f),
                    fontSize = 12,
                    leading = { IconRestart(TextMain) }
                )
            }
            Spacer(Modifier.weight(1f))
            NonogramGrid(puzzle, marks, onTap = { handleTap(it) }, onLong = { handleLong(it) })
            Spacer(Modifier.weight(1f))
            if (!state.cycleMode) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeButton(
                        label = "FILL",
                        selected = mode == 1,
                        fill = true,
                        modifier = Modifier.weight(1f)
                    ) { mode = 1 }
                    ModeButton(
                        label = "CROSS (X)",
                        selected = mode == 2,
                        fill = false,
                        modifier = Modifier.weight(1f)
                    ) { mode = 2 }
                }
            }
            Spacer(Modifier.height(53.dp))
        }

        if (showComplete) {
            CompletionOverlay(
                puzzle = puzzle,
                moves = moves,
                seconds = seconds,
                hasNext = levelIndex != null && levelIndex < Levels.count,
                onNext = {
                    if (levelIndex != null && levelIndex < Levels.count) {
                        val next = levelIndex + 1
                        state.screen = Screen.Play(Levels.puzzles[next - 1], "LEVEL $next", next)
                    }
                },
                onCoffee = { },
                onHome = { state.screen = Screen.LevelSelect }
            )
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    fill: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected && !fill) SelectPink else CardColorDeep
    val fg = if (selected && !fill) Color(0xFF2A1114) else TextMain
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                1.5.dp,
                if (selected && fill) TextMain else if (selected) SelectPink else BorderColor,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (fill) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(fg))
            } else {
                Canvas(Modifier.size(16.dp)) { drawCross(CrossColor, 0.12f) }
            }
            Spacer(Modifier.width(9.dp))
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed, letterSpacing = 0.6.sp)
        }
    }
}

@Composable
private fun NonogramGrid(puzzle: Puzzle, marks: List<Int>, onTap: (Int) -> Unit, onLong: (Int) -> Unit) {
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val hPad = 20.dp
    val rightPad = 44.dp
    val minClue = 40.dp
    val maxGridHeight = 470.dp
    var cell = minOf(49.dp, (screenW - hPad - rightPad - minClue) / puzzle.cols)
    cell = minOf(cell, maxGridHeight / puzzle.rows)
    val clueArea = screenW - hPad - rightPad - cell * puzzle.cols
    val maxColLines = puzzle.colClues.maxOf { it.size }.coerceAtLeast(1)
    val lineH = cell * 0.42f
    val clueFont = (cell.value * 0.30f).coerceIn(6.5f, 16f).sp
    val corner = RoundedCornerShape((cell.value * 0.14f).dp)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = hPad, end = rightPad)
    ) {
        Row {
            Spacer(Modifier.width(clueArea))
            Column {
                for (line in 0 until maxColLines) {
                    Row {
                        for (c in 0 until puzzle.cols) {
                            val clues = puzzle.colClues[c]
                            val startLine = maxColLines - clues.size
                            Box(Modifier.width(cell).height(lineH), contentAlignment = Alignment.Center) {
                                if (line >= startLine) {
                                    Text(
                                        "${clues[line - startLine]}",
                                        color = TextMain,
                                        fontSize = clueFont,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Condensed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Row {
            Column(Modifier.width(clueArea)) {
                for (r in 0 until puzzle.rows) {
                    Box(Modifier.fillMaxWidth().height(cell), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = puzzle.rowClues[r].joinToString(" "),
                            color = TextMain,
                            fontSize = clueFont,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Condensed,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
            Column {
                for (r in 0 until puzzle.rows) {
                    Row {
                        for (c in 0 until puzzle.cols) {
                            val index = r * puzzle.cols + c
                            val mark = marks[index]
                            Box(
                                Modifier
                                    .size(cell)
                                    .padding(0.6.dp)
                                    .clip(corner)
                                    .background(CardColor)
                                    .border(0.5.dp, BorderColor, corner)
                                    .pointerInput(index) {
                                        detectTapGestures(
                                            onTap = { onTap(index) },
                                            onLongPress = { onLong(index) }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (mark) {
                                    1 -> Box(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(cell * 0.035f)
                                            .clip(corner)
                                            .background(FillColor)
                                    )
                                    2 -> Canvas(Modifier.fillMaxSize()) { drawCross(CrossColor) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionOverlay(
    puzzle: Puzzle,
    moves: Int,
    seconds: Int,
    hasNext: Boolean,
    onNext: () -> Unit,
    onCoffee: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardColor)
                .padding(vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "LEVEL COMPLETED!",
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Condensed,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "MOVES: $moves • TIME: %02d:%02d".format(seconds / 60, seconds % 60),
                color = TextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Condensed,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .width((puzzle.cols * 20 + 24).dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardColorDeep)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    for (r in 0 until puzzle.rows) {
                        Row {
                            for (c in 0 until puzzle.cols) {
                                val on = puzzle.solution[r][c]
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .padding(1.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (on) FillColor else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            PillButton("NEXT LEVEL", onNext, Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(50.dp), filled = true, fontSize = 14)
            Spacer(Modifier.height(12.dp))
            PillButton(
                "BUY ME A COFFEE",
                { openUrl(context, "https://example.com") },
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(50.dp),
                fontSize = 14,
                leading = { IconCoffee(TextMain) }
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                "HOME",
                onHome,
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(50.dp),
                fontSize = 14,
                leading = { IconHome(TextMain) }
            )
        }
    }
}

@Composable
private fun Overlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = false) { }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Difficulty dialog used by RANDOM PUZZLE
// ---------------------------------------------------------------------------

@Composable
fun DifficultyDialog(onPick: (Difficulty) -> Unit, onDismiss: () -> Unit) {
    Overlay(onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardColor)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CHOOSE DIFFICULTY",
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Condensed,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "PLAY A DYNAMICALLY GENERATED NONOGRAM PUZZLE.",
                color = TextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Condensed,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            for ((i, d) in DIFFICULTIES.withIndex()) {
                PillButton(
                    d.label,
                    { onPick(d) },
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(48.dp),
                    filled = i == 0,
                    fontSize = 14
                )
                Spacer(Modifier.height(17.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("CANCEL", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Condensed)
            }
        }
    }
}
