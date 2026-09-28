package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

private val CardColor = Color(0xFF1E2230)
private val AccentTeal = Color(0xFF2AA79B)
private val LabelColor = Color(0xFFE4E7EF)
private val ValueColor = Color(0xFF3FC7B8)
private val TrackIdle = Color(0xFF2A3143)

/** Custom game dialog: grid size, mine count, fog of war and safe first tap. */
@Composable
fun CustomGameDialog(
    initial: GameConfig,
    onCancel: () -> Unit,
    onStart: (GameConfig) -> Unit
) {
    var size by remember { mutableStateOf(initial.size) }
    var mines by remember { mutableStateOf(initial.mines.coerceIn(1, (initial.size * initial.size - 9).coerceAtLeast(1))) }
    var fog by remember { mutableStateOf(initial.fog) }
    var safeFirst by remember { mutableStateOf(initial.safeFirstTap) }

    val maxMines = (size * size - 9).coerceAtLeast(1)
    val percent = mines * 100 / (size * size)

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardColor)
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Text("Custom Game", color = LabelColor, fontSize = 22.sp)
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Grid size", color = LabelColor, fontSize = 15.sp)
                Text("$size \u00D7 $size", color = ValueColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = size.toFloat(),
                onValueChange = { newValue ->
                    val newSize = newValue.roundToInt()
                    if (newSize != size) {
                        size = newSize
                        val newMax = (newSize * newSize - 9).coerceAtLeast(1)
                        mines = mines.coerceIn(1, newMax)
                    }
                },
                valueRange = 5f..20f,
                steps = 14,
                colors = sliderColors()
            )

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mines", color = LabelColor, fontSize = 15.sp)
                Text("$mines ($percent%)", color = ValueColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = mines.toFloat(),
                onValueChange = { mines = it.roundToInt().coerceIn(1, maxMines) },
                valueRange = 1f..maxMines.toFloat(),
                colors = sliderColors()
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fog of war", color = LabelColor, fontSize = 15.sp)
                Switch(checked = fog, onCheckedChange = { fog = it }, colors = switchColors())
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Safe first tap", color = LabelColor, fontSize = 15.sp)
                Switch(checked = safeFirst, onCheckedChange = { safeFirst = it }, colors = switchColors())
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = ValueColor, fontSize = 16.sp)
                }
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = {
                    onStart(
                        GameConfig(
                            difficulty = Difficulty.CUSTOM,
                            size = size,
                            mines = mines.coerceIn(1, maxMines),
                            fog = fog,
                            safeFirstTap = safeFirst,
                            timeLimit = null
                        )
                    )
                }) {
                    Text("Start", color = ValueColor, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = AccentTeal,
    activeTrackColor = AccentTeal,
    inactiveTrackColor = TrackIdle,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color(0xFF4A5268)
)

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color(0xFF0E2A26),
    checkedTrackColor = AccentTeal,
    checkedBorderColor = AccentTeal,
    uncheckedThumbColor = Color(0xFF8C93A6),
    uncheckedTrackColor = Color(0xFF39405A),
    uncheckedBorderColor = Color(0xFF39405A)
)

/** Licence information dialog reached from the link at the bottom of the board. */
@Composable
fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardColor)
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Text("Licenses", color = LabelColor, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = LICENSE_BODY,
                    color = Color(0xFFC6CBDA),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) {
                    Text("Close", color = ValueColor, fontSize = 16.sp)
                }
            }
        }
    }
}

private const val LICENSE_BODY = """Minesweeper Puzzle
Copyright (C) 2026 Blackbox Bench

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.

--------------------------------------------------------------------------
Bundled third-party components

Compose Toolkit (AndroidX) - Apache License 2.0
  Copyright 2024 The Android Open Source Project

Kotlin Standard Library - Apache License 2.0
  Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
  contributors.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
these files except in compliance with the License. You may obtain a copy of the
License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
"""
