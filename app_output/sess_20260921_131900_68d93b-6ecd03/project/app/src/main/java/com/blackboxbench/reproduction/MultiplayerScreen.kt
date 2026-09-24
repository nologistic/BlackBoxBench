package com.blackboxbench.reproduction

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MultiplayerScreen(
    difficulty: Difficulty,
    roomDigits: String,
    onDifficulty: (Difficulty) -> Unit,
    onRefreshCode: () -> Unit,
    onStart: (Difficulty, String) -> Unit,
    onJoin: (Difficulty, String) -> Unit,
    onBack: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var joinText by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf(false) }

    val roomCode = "${difficulty.upper}-$roomDigits"

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "MULTIPLAYER", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PanelCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    LabelText("DIFFICULTY")
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Difficulty.entries.take(4).forEach { entry ->
                            SelectableChip(
                                text = entry.upper,
                                selected = entry == difficulty,
                                onClick = { onDifficulty(entry) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectableChip(
                            text = Difficulty.EXPERT.upper,
                            selected = difficulty == Difficulty.EXPERT,
                            onClick = { onDifficulty(Difficulty.EXPERT) }
                        )
                    }
                }
            }

            PanelCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LabelText("YOUR ROOM CODE")
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = roomCode,
                        color = Palette.TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            StackButton(
                                text = "COPY CODE",
                                onClick = { clipboard.setText(AnnotatedString(copyForm(difficulty, roomDigits))) },
                                height = 52.dp,
                                leading = { IconCopy(size = 18.dp) }
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onRefreshCode() },
                            contentAlignment = Alignment.Center
                        ) {
                            IconRestart(size = 22.dp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    StackButton(
                        text = "START PUZZLE",
                        onClick = { onStart(difficulty, roomCode.uppercase()) },
                        primary = true,
                        height = 52.dp
                    )
                }
            }

            PanelCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    LabelText("JOIN WITH CODE")
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(Metrics.radiusSmall))
                            .background(Palette.CardHigh)
                            .border(
                                1.dp,
                                if (joinError) Palette.Red else Palette.BorderSoft,
                                RoundedCornerShape(Metrics.radiusSmall)
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = joinText,
                            onValueChange = {
                                joinText = it
                                joinError = false
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Palette.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            cursorBrush = SolidColor(Palette.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (joinText.isEmpty()) {
                            Text(
                                text = "E.G. EASY-123456",
                                color = Palette.TextMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconClipboard(size = 18.dp)
                        }
                    }
                    if (joinError) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "FORMAT: DIFFICULTY-CODE (E.G. EASY-123456)",
                            color = Color(0xFFE0736A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    StackButton(
                        text = "JOIN PUZZLE",
                        onClick = {
                            val parsed = parseRoomCode(joinText)
                            if (parsed == null) {
                                joinError = true
                            } else {
                                joinError = false
                                onJoin(parsed.first, parsed.second)
                            }
                        },
                        height = 52.dp
                    )
                }
            }
        }
    }
}

/** The clipboard receives the title-cased form, matching the observed copy behaviour. */
private fun copyForm(difficulty: Difficulty, digits: String): String =
    "${difficulty.label}-$digits"

/**
 * Accepts any separator the keyboard may produce (or none at all) by keeping only
 * letters and digits, then requiring a known difficulty followed by six digits.
 */
fun parseRoomCode(raw: String): Pair<Difficulty, String>? {
    val cleaned = raw.filter { it.isLetterOrDigit() }
    val match = Regex("^([A-Za-z]+)(\\d{6})$").find(cleaned) ?: return null
    val difficulty = Difficulty.fromToken(match.groupValues[1]) ?: return null
    return difficulty to "${difficulty.upper}-${match.groupValues[2]}"
}
