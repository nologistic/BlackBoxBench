package com.blackboxbench.reproduction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: Store,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit
) {
    store.version.value
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingRow(Icons.Default.Refresh, "同步", "Tasks.org Cloud、Google Tasks…") { }
            SettingRow(Icons.Default.Create, "外观", "主题、强调色") { onOpenAppearance() }
            SettingRow(Icons.Default.Notifications, "通知", "提醒通知、安静时段") { }
            SettingRow(Icons.Default.Info, "提醒", "提醒响铃、振动") { }
            SettingRow(Icons.Default.Build, "电池优化", "忽略电池优化") { }
            SettingRow(Icons.Default.Share, "备份", "导入与导出备份") { }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(store: Store, onBack: () -> Unit) {
    store.version.value
    var chosen by remember { mutableStateOf(store.data.prefs.theme) }
    val options = listOf(
        "system" to "系统默认", "light" to "浅色主题",
        "dark" to "深色主题", "black" to "黑色主题"
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "主题",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            for ((key, label) in options) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    border = if (store.data.prefs.theme == key)
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else null
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { store.setTheme(key); chosen = key }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val preview = when (key) {
                            "light" -> Color(0xFFFBF9F5)
                            "dark" -> Color(0xFF1A1C1E)
                            "black" -> Color.Black
                            else -> Color(0xFF9E9E9E)
                        }
                        Spacer(
                            Modifier
                                .size(28.dp)
                                .padding(2.dp)
                        )
                        Card(
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = preview
                            )
                        ) {}
                        Spacer(Modifier.width(14.dp))
                        Text(label, modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = store.data.prefs.theme == key,
                            onClick = { store.setTheme(key); chosen = key }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "强调色",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row {
                for (c in listOf(Color(0xFF1565C0), Color(0xFF00897B), Color(0xFF8E24AA),
                    Color(0xFFEF6C00))) {
                    Card(
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(40.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = c
                        )
                    ) {}
                }
            }
        }
    }
}
