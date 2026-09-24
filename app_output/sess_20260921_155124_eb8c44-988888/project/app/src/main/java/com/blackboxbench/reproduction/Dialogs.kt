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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

data class CustomConfig(
    val size: Int = 16,
    val mines: Int = 40,
    val fog: Boolean = false,
    val safeFirstTap: Boolean = true
)

@Composable
fun CustomGameDialog(
    initial: CustomConfig,
    onCancel: () -> Unit,
    onStart: (CustomConfig) -> Unit
) {
    var size by remember { mutableStateOf(initial.size) }
    var mines by remember { mutableStateOf(initial.mines) }
    var fog by remember { mutableStateOf(initial.fog) }
    var safe by remember { mutableStateOf(initial.safeFirstTap) }

    val cap = maxMinesFor(size * size)
    val effectiveMines = mines.coerceIn(1, cap)
    val percent = effectiveMines * 100 / (size * size)

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .background(Palette.Panel, RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text("Custom Game", color = Palette.TextMain, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            LabeledValue("Grid size", "$size \u00D7 $size")
            Slider(
                value = size.toFloat(),
                onValueChange = { size = it.roundToInt().coerceIn(6, 20) },
                valueRange = 6f..20f,
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = Palette.PanelActive,
                    activeTrackColor = Palette.PanelActive,
                    inactiveTrackColor = Palette.TabTitleIdle.copy(alpha = 0.35f),
                    activeTickColor = Palette.Panel,
                    inactiveTickColor = Palette.Panel
                )
            )
            Spacer(Modifier.height(6.dp))
            LabeledValue("Mines", "$effectiveMines ($percent%)")
            Slider(
                value = effectiveMines.toFloat(),
                onValueChange = { mines = it.roundToInt().coerceIn(1, cap) },
                valueRange = 1f..cap.toFloat(),
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = Palette.PanelActive,
                    activeTrackColor = Palette.PanelActive,
                    inactiveTrackColor = Palette.TabTitleIdle.copy(alpha = 0.35f),
                    activeTickColor = Palette.Panel,
                    inactiveTickColor = Palette.Panel
                )
            )
            Spacer(Modifier.height(10.dp))
            ToggleRow("Fog of war", fog) { fog = it }
            Spacer(Modifier.height(4.dp))
            ToggleRow("Safe first tap", safe) { safe = it }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Palette.TabTitleIdle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { onStart(CustomConfig(size, effectiveMines, fog, safe)) }) {
                    Text("Start", color = Palette.PanelActive, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Palette.TextMain, fontSize = 15.sp)
        Text(value, color = Palette.PanelActive, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Palette.TextMain, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.Panel,
                checkedTrackColor = Palette.PanelActive,
                uncheckedThumbColor = Palette.TabTitleIdle,
                uncheckedTrackColor = Palette.Background
            )
        )
    }
}

private const val LICENSE_TEXT = """Minesweeper is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Minesweeper is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

The following libraries are used under the Apache License, Version 2.0. Their notices and the full license text follow.

    androidx.compose.ui, androidx.compose.foundation, androidx.compose.animation
    androidx.compose.material3
    androidx.activity:activity-compose
    androidx.core:core-ktx
    Copyright (c) The Android Open Source Project

    Kotlin standard library
    Copyright (c) JetBrains s.r.o.

Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.

"Legal Entity" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity.

"You" (or "Your") shall mean an individual or Legal Entity exercising permissions granted by this License.

"Source" form shall mean the preferred form for making modifications, including but not limited to software source code, documentation source, and configuration files.

"Object" form shall mean any form resulting from mechanical transformation or translation of a Source form, including but not limited to compiled object code, generated documentation, and conversions to other media types.

"Work" shall mean the work of authorship, whether in Source or Object form, made available under the License, as indicated by a copyright notice that is included in or attached to the work.

"Derivative Works" shall mean any work, whether in Source or Object form, that is based on (or derived from) the Work and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship.

"Contribution" shall mean any work of authorship, including the original version of the Work and any modifications or additions to that Work or Derivative Works thereof, that is intentionally submitted to Licensor for inclusion in the Work by the copyright owner or by an individual or Legal Entity authorized to submit on behalf of the copyright owner.

"Contributor" shall mean Licensor and any individual or Legal Entity on behalf of whom a Contribution has been received by Licensor and subsequently incorporated within the Work.

2. Grant of Copyright License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable (except as stated in this section) patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work.

4. Redistribution. You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, and in Source or Object form, provided that You meet the following conditions: (a) You must give any other recipients of the Work or Derivative Works a copy of this License; and (b) You must cause any modified files to carry prominent notices stating that You changed the files; and (c) You must retain, in the Source form of any Derivative Works that You distribute, all copyright, patent, trademark, and attribution notices from the Source form of the Work; and (d) If the Work includes a "NOTICE" text file as part of its distribution, then any Derivative Works that You distribute must include a readable copy of the attribution notices contained within such NOTICE file.

5. Submission of Contributions. Unless You explicitly state otherwise, any Contribution intentionally submitted for inclusion in the Work by You to the Licensor shall be under the terms and conditions of this License, without any additional terms or conditions.

6. Trademarks. This License does not grant permission to use the trade names, trademarks, service marks, or product names of the Licensor, except as required for reasonable and customary use in describing the origin of the Work and reproducing the content of the NOTICE file.

7. Disclaimer of Warranty. Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

8. Limitation of Liability. In no event and under no legal theory, whether in tort (including negligence), contract, or otherwise, unless required by applicable law (such as deliberate and grossly negligent acts) or agreed to in writing, shall any Contributor be liable to You for damages.

9. Accepting Warranty or Additional Liability. While redistributing the Work or Derivative Works thereof, You may choose to offer, and charge a fee for, acceptance of support, warranty, indemnity, or other liability obligations and/or rights consistent with this License.

END OF TERMS AND CONDITIONS"""

@Composable
fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .background(Palette.Panel, RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text("Licenses", color = Palette.TextMain, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(LICENSE_TEXT, color = Palette.TabTitleIdle, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) {
                    Text("Close", color = Palette.PanelActive, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
