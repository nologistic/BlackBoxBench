package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    currentLevel: Int,
    onPlay: () -> Unit,
    onSelectLevel: () -> Unit,
    onRandom: () -> Unit,
    onMultiplayer: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 22.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleBadge { IconStar(size = 22.dp) }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(78.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Palette.Border, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LEVEL $currentLevel",
                    color = Palette.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.weight(1f))
            CircleBadge { IconHeart(size = 22.dp) }
        }

        Spacer(Modifier.height(80.dp))

        Text(
            text = "NONOGRAM",
            color = Palette.TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "PICTURE LOGIC PUZZLE",
            color = Palette.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Metrics.screenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StackButton(text = "PLAY", onClick = onPlay, primary = true)
            StackButton(text = "SELECT LEVEL", onClick = onSelectLevel)
            StackButton(text = "RANDOM PUZZLE", onClick = onRandom)
            StackButton(text = "MULTIPLAYER", onClick = onMultiplayer)
            StackButton(text = "HOW TO PLAY", onClick = onHowToPlay)
            StackButton(text = "SETTINGS", onClick = onSettings)
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun CircleBadge(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Palette.Card)
            .border(1.dp, Palette.Border, CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "HOW TO PLAY", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HowToCard(
                icon = { IconGridGlyph(size = 26.dp) },
                title = "1. REVEAL THE PICTURE",
                body = "NONOGRAMS ARE LOGIC PUZZLES WHERE GRID CELLS MUST BE FILLED OR LEFT " +
                    "BLANK ACCORDING TO NUMBERS AT THE SIDE OF THE GRID."
            )
            HowToCard(
                icon = { IconLinesGlyph(size = 26.dp) },
                title = "2. READ THE CLUES",
                body = "THE NUMBERS SHOW SEQUENCES OF FILLED CELLS IN THAT ROW OR COLUMN. " +
                    "E.G., \"3 1\" MEANS A BLOCK OF 3 FILLED CELLS FOLLOWED BY 1 FILLED CELL."
            )
            HowToCard(
                icon = { IconCrossGlyph(size = 26.dp) },
                title = "3. MARK BLANK SPACES",
                body = "TAP A CELL TO FILL IT, OR MARK EMPTY SPACES WITH AN \"X\" TO KEEP " +
                    "TRACK OF SPACES THAT CANNOT BE FILLED."
            )
            HowToCard(
                icon = { IconTrophy(size = 26.dp) },
                title = "4. COMPLETE THE GRID",
                body = "SOLVE THE ENTIRE PUZZLE USING LOGIC WITHOUT GUESSING TO REVEAL THE " +
                    "HIDDEN PIXEL IMAGE!"
            )
        }
    }
}

@Composable
private fun HowToCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String
) {
    PanelCard {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Palette.CardHigh),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = Palette.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    color = Palette.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    haptic: Boolean,
    longPressToCross: Boolean,
    cycleMode: Boolean,
    onHaptic: (Boolean) -> Unit,
    onLongPress: (Boolean) -> Unit,
    onCycleMode: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "SETTINGS", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            PanelCard {
                Column {
                    SwitchRow("HAPTIC FEEDBACK", haptic, onHaptic)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Palette.BorderSoft)
                    ) {}
                    SwitchRow("LONG PRESS TO CROSS", longPressToCross, onLongPress)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Palette.BorderSoft)
                    ) {}
                    SwitchRow("CYCLE MODE", cycleMode, onCycleMode)
                }
            }
            Spacer(Modifier.height(18.dp))
            StackButton(
                text = "RESET PROGRESS",
                onClick = onResetProgress,
                leading = { IconRestart(size = 18.dp) }
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (checked) Palette.TextPrimary else Palette.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.weight(1f)
        )
        NonoSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun ConfirmResetDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    DialogScaffold(onDismiss = onCancel) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconWarningBadge(size = 52.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "RESET PROGRESS?",
                color = Palette.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "ARE YOU SURE YOU WANT TO RESET ALL YOUR GAME PROGRESS? " +
                    "THIS ACTION CANNOT BE UNDONE.",
                color = Palette.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 0.4.sp
            )
            Spacer(Modifier.height(20.dp))
            StackButton(text = "RESET PROGRESS", onClick = onConfirm, primary = true, height = 52.dp)
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(Metrics.radiusLarge))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CANCEL",
                    color = Palette.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SelectLevelScreen(
    completed: Set<Int>,
    highestUnlocked: Int,
    onBack: () -> Unit,
    onPick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "SELECT LEVEL", onBack = onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items((1..Levels.count).toList()) { level ->
                LevelTile(
                    level = level,
                    completed = completed.contains(level),
                    locked = level > highestUnlocked,
                    onClick = { if (level <= highestUnlocked) onPick(level) }
                )
            }
        }
    }
}

@Composable
private fun LevelTile(
    level: Int,
    completed: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        completed -> Palette.White
        locked -> Palette.BorderSoft
        else -> Palette.Border
    }
    val borderWidth = if (completed) 2.dp else 1.dp
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(Metrics.radiusLarge))
            .background(if (locked) Color(0xFF1B1B1B) else Palette.Tile)
            .border(borderWidth, borderColor, RoundedCornerShape(Metrics.radiusLarge))
            .clickable(enabled = !locked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (locked) {
            IconLock(size = 22.dp, color = Palette.TextMuted)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$level",
                    color = Palette.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (completed) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Palette.CardHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        IconCheck(size = 12.dp)
                    }
                }
            }
        }
    }
}
