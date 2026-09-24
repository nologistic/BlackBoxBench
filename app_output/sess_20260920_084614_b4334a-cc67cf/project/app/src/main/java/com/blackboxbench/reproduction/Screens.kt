package com.blackboxbench.reproduction

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun HomeScreen(
    currentLevel: Int,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
    onRandom: () -> Unit,
    onMultiplayer: () -> Unit,
    onHowTo: () -> Unit,
    onSettings: () -> Unit
) {
    ScreenRoot {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleBadge("★", AppYellow)
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .border(1.dp, AppBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 21.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TallText("LEVEL $currentLevel", size = 15, align = TextAlign.Center)
                }
                CircleBadge("♥", AppRed)
            }
            Spacer(Modifier.weight(0.88f))
            TallText("NONOGRAM", size = 48, align = TextAlign.Center, spacing = 0f)
            TallText(
                "PICTURE LOGIC PUZZLE",
                size = 14,
                color = AppMuted,
                align = TextAlign.Center,
                spacing = 1.6f,
                modifier = Modifier.padding(top = 14.dp)
            )
            Spacer(Modifier.weight(1.05f))
            MenuButton("PLAY", onPlay, primary = true, height = 58.dp)
            Spacer(Modifier.height(10.dp))
            MenuButton("SELECT LEVEL", onSelect)
            Spacer(Modifier.height(10.dp))
            MenuButton("RANDOM PUZZLE", onRandom)
            Spacer(Modifier.height(10.dp))
            MenuButton("MULTIPLAYER", onMultiplayer)
            Spacer(Modifier.height(10.dp))
            MenuButton("HOW TO PLAY", onHowTo)
            Spacer(Modifier.height(10.dp))
            MenuButton("SETTINGS", onSettings)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun HowToScreen(onBack: () -> Unit) {
    val cards = listOf(
        Triple("▦", "1. REVEAL THE PICTURE", "NONOGRAMS ARE LOGIC PUZZLES WHERE GRID CELLS MUST BE FILLED OR LEFT BLANK ACCORDING TO NUMBERS AT THE SIDE OF THE GRID."),
        Triple("≡", "2. READ THE CLUES", "THE NUMBERS SHOW SEQUENCES OF FILLED CELLS IN THAT ROW OR COLUMN. E.G., “3 1” MEANS A BLOCK OF 3 FILLED CELLS FOLLOWED BY 1 FILLED CELL."),
        Triple("×", "3. MARK BLANK SPACES", "TAP A CELL TO FILL IT, OR MARK EMPTY SPACES WITH AN “X” TO KEEP TRACK OF SPACES THAT CANNOT BE FILLED."),
        Triple("♜", "4. COMPLETE THE GRID", "SOLVE THE ENTIRE PUZZLE USING LOGIC WITHOUT GUESSING TO REVEAL THE HIDDEN PIXEL IMAGE!")
    )
    ScreenRoot {
        Column(Modifier.fillMaxSize()) {
            BackHeader("HOW TO PLAY", onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                cards.forEach { (icon, title, body) ->
                    Panel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color(0xFF444444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                TallText(icon, size = 28, spacing = 0f, align = TextAlign.Center)
                            }
                            Column(Modifier.padding(start = 18.dp)) {
                                TallText(title, size = 17)
                                TallText(
                                    body,
                                    size = 12,
                                    color = AppMuted,
                                    modifier = Modifier.padding(top = 8.dp),
                                    spacing = 0.2f
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    haptic: Boolean,
    longPress: Boolean,
    cycle: Boolean,
    onHaptic: (Boolean) -> Unit,
    onLongPress: (Boolean) -> Unit,
    onCycle: (Boolean) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    ScreenRoot {
        Column(Modifier.fillMaxSize()) {
            BackHeader("SETTINGS", onBack)
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Panel(Modifier.fillMaxWidth()) {
                    Column {
                        ToggleRow("HAPTIC FEEDBACK", haptic, onHaptic)
                        SectionDivider()
                        ToggleRow("LONG PRESS TO CROSS", longPress, onLongPress)
                        SectionDivider()
                        ToggleRow("CYCLE MODE", cycle, onCycle)
                    }
                }
                Spacer(Modifier.height(18.dp))
                MenuButton("RESET PROGRESS", onReset, symbol = "↶", height = 56.dp)
            }
        }
    }
}

@Composable
fun ResetProgressDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TallText("⚠", size = 39, color = AppRed, spacing = 0f)
                TallText("RESET PROGRESS?", size = 23, modifier = Modifier.padding(top = 12.dp), align = TextAlign.Center)
                TallText(
                    "ARE YOU SURE YOU WANT TO RESET ALL YOUR GAME PROGRESS?\nTHIS ACTION CANNOT BE UNDONE.",
                    size = 13,
                    color = AppMuted,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Spacer(Modifier.height(20.dp))
                MenuButton("RESET PROGRESS", onConfirm, primary = true, height = 58.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    TallText("CANCEL", size = 15, color = AppMuted, align = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun SelectLevelScreen(currentLevel: Int, onLevel: (Int) -> Unit, onBack: () -> Unit) {
    val total = currentLevel + 10
    ScreenRoot {
        Column(Modifier.fillMaxSize()) {
            BackHeader("SELECT LEVEL", onBack)
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                (0 until ((total + 3) / 4)).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        repeat(4) { col ->
                            val index = row * 4 + col + 1
                            if (index <= total) {
                                LevelCard(
                                    modifier = Modifier.weight(1f),
                                    level = index,
                                    completed = index < currentLevel,
                                    unlocked = index <= currentLevel,
                                    onClick = { if (index <= currentLevel) onLevel(index) }
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    modifier: Modifier,
    level: Int,
    completed: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (completed) AppWhite else AppBorder
    Box(
        modifier = modifier
            .height(78.dp)
            .background(if (unlocked) Color(0xFF202020) else Color(0xFF151515), RoundedCornerShape(14.dp))
            .border(if (unlocked) 1.dp else 0.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = unlocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!unlocked) {
            TallText("▣", size = 23, color = Color(0xFFAFAFAF), spacing = 0f)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TallText(level.toString(), size = 20, align = TextAlign.Center)
                if (completed) TallText("✓", size = 15, align = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DifficultyDialog(onChoose: (Difficulty) -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TallText("CHOOSE DIFFICULTY", size = 24, align = TextAlign.Center)
                TallText(
                    "PLAY A DYNAMICALLY GENERATED NONOGRAM PUZZLE.",
                    size = 13,
                    color = AppMuted,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
                Difficulty.entries.forEachIndexed { index, difficulty ->
                    MenuButton(
                        difficulty.title,
                        onClick = { onChoose(difficulty) },
                        primary = index == 0,
                        height = 55.dp
                    )
                    Spacer(Modifier.height(9.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    TallText("CANCEL", size = 15, color = AppMuted, align = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun MultiplayerScreen(
    selected: Difficulty,
    roomDigits: String,
    joinCode: String,
    onDifficulty: (Difficulty) -> Unit,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onJoinCode: (String) -> Unit,
    onStart: () -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit
) {
    val code = selected.title + "-" + roomDigits
    ScreenRoot {
        Column(Modifier.fillMaxSize()) {
            BackHeader("MULTIPLAYER", onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        TallText("DIFFICULTY", size = 14, color = AppMuted)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Difficulty.entries.take(4).forEach { difficulty ->
                                DifficultyChip(
                                    modifier = Modifier.weight(1f),
                                    difficulty = difficulty,
                                    selected = difficulty == selected,
                                    onClick = { onDifficulty(difficulty) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        DifficultyChip(
                            modifier = Modifier.width(78.dp),
                            difficulty = Difficulty.EXPERT,
                            selected = selected == Difficulty.EXPERT,
                            onClick = { onDifficulty(Difficulty.EXPERT) }
                        )
                    }
                }
                Panel(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TallText("YOUR ROOM CODE", size = 14, color = AppMuted, align = TextAlign.Center)
                        TallText(code, size = 27, modifier = Modifier.padding(top = 14.dp), align = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MenuButton("COPY CODE", onCopy, symbol = "▣", modifier = Modifier.weight(1f), height = 55.dp)
                            Box(
                                modifier = Modifier
                                    .size(55.dp)
                                    .clickable(onClick = onRefresh),
                                contentAlignment = Alignment.Center
                            ) {
                                TallText("↻", size = 31, spacing = 0f)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        MenuButton("START PUZZLE", onStart, primary = true, height = 58.dp)
                    }
                }
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        TallText("JOIN WITH CODE", size = 14, color = AppMuted)
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { onJoinCode(it.uppercase()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            placeholder = { TallText("E.G. EASY-123456", size = 15, color = Color(0xFF5C5C5C)) },
                            trailingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clickable(onClick = onPaste),
                                    contentAlignment = Alignment.Center
                                ) { TallText("▣", size = 24, color = AppWhite, spacing = 0f) }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = AppWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            ),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            shape = RoundedCornerShape(13.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppWhite,
                                unfocusedBorderColor = AppBorder,
                                cursorColor = AppWhite,
                                focusedContainerColor = Color(0xFF151515),
                                unfocusedContainerColor = Color(0xFF151515)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        MenuButton("JOIN PUZZLE", onJoin, height = 56.dp)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun DifficultyChip(
    modifier: Modifier,
    difficulty: Difficulty,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(43.dp),
        shape = RoundedCornerShape(10.dp),
        border = if (selected) null else BorderStroke(1.dp, AppBorder),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AppWhite else AppPanel,
            contentColor = if (selected) Color.Black else AppWhite
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        if (selected) TallText("✓", size = 12, color = Color.Black, spacing = 0f)
        TallText(
            difficulty.title,
            size = 11,
            color = if (selected) Color.Black else AppWhite,
            spacing = 0f,
            modifier = if (selected) Modifier.padding(start = 3.dp) else Modifier
        )
    }
}
