package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val DLG_BG = Color(0xFF1B2635)

@Composable
fun CustomGameDialog(initial: DifficultyConfig, onStart: (DifficultyConfig) -> Unit, onCancel: () -> Unit) {
    var w by remember { mutableStateOf(initial.cols) }
    var h by remember { mutableStateOf(initial.rows) }
    var m by remember { mutableStateOf(initial.mines) }

    fun decW() { if (w > 1 && (w - 1) * h > m) w-- }
    fun decH() { if (h > 1 && w * (h - 1) > m) h-- }
    fun decM() { if (m > 0) m-- }
    fun incM() {
        if (m + 1 >= w * h) { if (w < 99) w++; if (h < 99) h++ }
        m++
    }

    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.background(DLG_BG, RoundedCornerShape(20.dp)).padding(20.dp).width(300.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Custom game", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(30.dp).background(Color(0xFF22344A), CircleShape)
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center) { Text("✕", color = Color.White, fontSize = 14.sp) }
            }
            Spacer(Modifier.height(16.dp))
            StepperRow("Width", w, onMinus = ::decW, onPlus = { if (w < 99) w++ })
            StepperRow("Height", h, onMinus = ::decH, onPlus = { if (h < 99) h++ })
            StepperRow("Mines", m, onMinus = ::decM, onPlus = ::incM)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33507A)),
                    onClick = { onStart(DifficultyConfig(w, h, m)) },
                    modifier = Modifier.weight(1f)) { Text("Start game") }
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = PILL_TEXT_REF) }
            }
        }
    }
}

val PILL_TEXT_REF = Color(0xFFB9C6D6)

@Composable
private fun StepperRow(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFC7D2DE), fontSize = 16.sp, modifier = Modifier.weight(1f))
        Box(Modifier.size(34.dp).background(Color(0xFF22344A), CircleShape).clickable(onClick = onMinus),
            contentAlignment = Alignment.Center) { Text("−", color = Color.White, fontSize = 18.sp) }
        Text("$value", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(56.dp))
        Box(Modifier.size(34.dp).background(Color(0xFF22344A), CircleShape).clickable(onClick = onPlus),
            contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 18.sp) }
    }
}
