package com.blackboxbench.reproduction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

private val TEAL = Color(0xFF009688)

/** 首次启动欢迎页：添加账号 / 继续但不同步 / 导入备份 */
@Composable
fun WelcomeScreen(onContinueWithoutSync: () -> Unit) {
    var showAccountDialog by remember { mutableStateOf(false) }
    var offlineHint by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(200.dp))
        // logo
        Box(
            modifier = Modifier.size(88.dp).background(TEAL, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Tasks", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Spacer(Modifier.height(8.dp))
        Text("保持井然有序", fontSize = 15.sp, color = Color(0xFF757575))
        Text("管理您的待办清单、标签与提醒", fontSize = 15.sp, color = Color(0xFF757575))

        Spacer(Modifier.height(320.dp))

        Button(
            onClick = { showAccountDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = TEAL),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
        ) { Text("添加账号", fontSize = 16.sp) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onContinueWithoutSync,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
        ) { Text("继续但不同步", fontSize = 16.sp) }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onContinueWithoutSync) { Text("导入 Tasks.org 备份", color = Color(0xFF009688)) }
        Spacer(Modifier.height(24.dp))
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("添加账号") },
            text = {
                Column {
                    listOf("Google Tasks", "CalDAV", "EteSync", "DecSync CC", "从文件备份恢复").forEach { name ->
                        TextButton(
                            onClick = { offlineHint = true; showAccountDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(name, color = Color(0xFF212121)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) { Text("取消") }
            },
        )
    }
    if (offlineHint) {
        AlertDialog(
            onDismissRequest = { offlineHint = false },
            title = { Text("无法连接网络") },
            text = { Text("设备处于离线状态，无法添加账号。请检查网络后重试，或选择“继续但不同步”。") },
            confirmButton = { TextButton(onClick = { offlineHint = false }) { Text("好的") } },
        )
    }
}
