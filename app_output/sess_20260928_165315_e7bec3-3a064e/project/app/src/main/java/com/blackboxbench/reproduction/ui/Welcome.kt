package com.blackboxbench.reproduction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackboxbench.reproduction.Prefs
import com.blackboxbench.reproduction.StateStore

@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    var showOffline by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))
            TasksLogo(size = 110)
            Spacer(Modifier.height(16.dp))
            Text("Tasks", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3C4043))
            Spacer(Modifier.height(48.dp))

            WelcomeOption(
                iconGlyph = "👤",
                title = "添加账号",
                subtitle = "同步 Google Tasks、CalDAV 等"
            ) {
                showOffline = true
            }
            Spacer(Modifier.height(8.dp))
            WelcomeOption(
                iconGlyph = "🚫",
                title = "继续但不同步",
                subtitle = "任务只保存在这台设备上"
            ) {
                Prefs.putStr("welcome_choice", "nosync")
                StateStore.createEmptyDefault()
                Prefs.putStr("last_view", "mytasks")
                onDone()
            }
            Spacer(Modifier.height(8.dp))
            WelcomeOption(
                iconGlyph = "📥",
                title = "导入 Tasks.org 备份",
                subtitle = "从备份文件恢复任务"
            ) {
                Prefs.putStr("welcome_choice", "import")
                StateStore.importSeed(ctx)
                Prefs.putStr("last_view", "mytasks")
                onDone()
            }
        }
    }

    if (showOffline) {
        OfflineAccountDialog(onDismiss = { showOffline = false })
    }
}

@Composable
private fun WelcomeOption(iconGlyph: String, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = Color(0xFFF1F3F7)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(iconGlyph, fontSize = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, color = Color(0xFF202124))
                Text(subtitle, fontSize = 13.sp, color = Color(0xFF80868B))
            }
        }
    }
}
