package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

private val DialogBackground = Color(0xFF1B2735)
private val DialogText = Color(0xFFE7EEF6)
private val DialogMuted = Color(0xFFC2CEDA)
private val DialogAccent = Color(0xFF26D3A2)
private val SliderTrack = Color(0xFF2E4152)

@Composable
fun CustomGameDialog(
    size: Int,
    mines: Int,
    fog: Boolean,
    safeFirstTap: Boolean,
    onSizeChange: (Int) -> Unit,
    onMinesChange: (Int) -> Unit,
    onFogChange: (Boolean) -> Unit,
    onSafeChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onStart: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DialogBackground)
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Text("Custom Game", color = DialogText, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(20.dp))
            LabelValueRow("Grid size", "$size x $size")
            Slider(
                value = size.toFloat(),
                onValueChange = { onSizeChange(it.roundToInt()) },
                valueRange = 5f..20f,
                steps = 14,
                colors = sliderColors()
            )
            Spacer(Modifier.height(8.dp))
            val cells = size * size
            val percent = (mines * 100f / cells).roundToInt()
            LabelValueRow("Mines", "$mines ($percent%)")
            Slider(
                value = mines.toFloat(),
                onValueChange = { onMinesChange(it.roundToInt().coerceIn(1, maxMinesFor(size))) },
                valueRange = 1f..maxMinesFor(size).toFloat(),
                colors = sliderColors()
            )
            Spacer(Modifier.height(18.dp))
            SwitchRow("Fog of war", fog, onFogChange)
            Spacer(Modifier.height(10.dp))
            SwitchRow("Safe first tap", safeFirstTap, onSafeChange)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color(0xFF9DB0C3), fontSize = 15.sp)
                }
                Spacer(Modifier.width(10.dp))
                TextButton(onClick = onStart) {
                    Text("Start", color = DialogAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DialogText, fontSize = 15.sp)
        Text(value, color = DialogText, fontSize = 15.sp)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = DialogText, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DialogAccent,
                checkedBorderColor = DialogAccent,
                uncheckedThumbColor = Color(0xFFB0BEC5),
                uncheckedTrackColor = Color(0xFF37474F),
                uncheckedBorderColor = Color(0xFF546E7A)
            )
        )
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = DialogAccent,
    activeTrackColor = DialogAccent,
    inactiveTrackColor = SliderTrack,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent
)

@Composable
fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DialogBackground)
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Text("Licenses", color = DialogText, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = LICENSE_TEXT,
                    color = DialogMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) {
                    Text("Close", color = DialogAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val LICENSE_TEXT = """
Minesweeper is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

The libraries below are used under the Apache License 2.0. Their attribution and
the full license text follow.

    androidx.compose (ui, ui-graphics, foundation, animation)
    androidx.compose.material3
    androidx.activity:activity-compose
    androidx.core:core-ktx
        Copyright (C) The Android Open Source Project

    Kotlin standard library
        Copyright (C) JetBrains s.r.o.

Apache License
Version 2.0,
January 2004

http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
""".trimIndent()
