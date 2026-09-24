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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SelectScreen(
    completed: Set<Int>,
    message: String?,
    onBack: () -> Unit,
    onPickLevel: (Int) -> Unit,
    onLocked: () -> Unit
) {
    val maxCompleted = completed.maxOrNull() ?: 0
    val current = if (maxCompleted >= Levels.TOTAL) Levels.TOTAL else maxCompleted + 1

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { Text("←", color = TextWhite, fontSize = 26.sp) }
            Spacer(Modifier.weight(1f))
            Text("SELECT LEVEL", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                color = MarkRed,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            userScrollEnabled = false
        ) {
            items((1..Levels.TOTAL).toList()) { level ->
                val isCompleted = level in completed
                val isCurrent = level == current
                val isLocked = level > current
                val cardBg = when {
                    isCurrent -> Color(0xFF2A3A66)
                    isLocked -> Color(0xFF20203A)
                    else -> PanelColor
                }
                val border = when {
                    isCurrent -> BlueCurrent
                    isCompleted -> GreenOk
                    else -> Color(0xFF3A3A5C)
                }
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .background(cardBg, RoundedCornerShape(14.dp))
                        .border(1.5.dp, border, RoundedCornerShape(14.dp))
                        .clickable {
                            if (isLocked) onLocked() else onPickLevel(level)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$level",
                            color = if (isLocked) TextDim else TextWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        when {
                            isCompleted -> Text("✓", color = GreenOk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            isLocked -> Text("🔒", fontSize = 16.sp)
                            else -> Spacer(Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}
