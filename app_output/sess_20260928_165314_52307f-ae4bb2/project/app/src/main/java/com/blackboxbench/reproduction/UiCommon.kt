package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 支持 long-press 的行点击 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick, onLongClick = onLongClick)

/** 排序对话框：我的排序/截止日期/创建日期/修改日期/标题/优先级 + 升序/降序 */
@Composable
fun SortDialog(initialKey: String, initialAscending: Boolean, onApply: (String, Boolean) -> Unit, onDismiss: () -> Unit) {
    var key by remember { mutableStateOf(initialKey) }
    var asc by remember { mutableStateOf(initialAscending) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序") },
        text = {
            Column {
                listOf(
                    "my" to "我的排序",
                    "due" to "截止日期",
                    "created" to "创建日期",
                    "modified" to "修改日期",
                    "title" to "标题",
                    "priority" to "优先级",
                ).forEach { (k, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        RadioButton(selected = key == k, onClick = { key = k })
                        Text(label, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (asc) "升序" else "降序", fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(checked = asc, onCheckedChange = { asc = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(key, asc) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun RenameListDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名列表") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
