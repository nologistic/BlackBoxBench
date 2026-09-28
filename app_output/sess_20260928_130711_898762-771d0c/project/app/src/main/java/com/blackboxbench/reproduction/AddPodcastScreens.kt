package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Tile = Color(0xFFC6C6C6)

@Composable
fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "搜索播客...",
    onSearch: () -> Unit = {},
    autoFocus: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) {
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "清除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onValueChange("") },
                )
            }
        }
    }
}

@Composable
fun AddPodcastScreen(
    onBack: () -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onRss: () -> Unit,
    onLocalFolder: () -> Unit,
    onApple: () -> Unit,
    onFyyd: () -> Unit,
    onIndex: () -> Unit,
    onOpml: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("添加播客", onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            SearchPill(value = query, onValueChange = onQuery, onSearch = onSearchSubmit)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(4) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Tile)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {},
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("显示建议")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Apple Podcasts 提供的建议",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text("发现更多 »", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
        }
        Column {
            OptionRow(AppIcons.Rss, "通过 RSS 地址添加播客", onRss)
            OptionRow(AppIcons.Folder, "添加本地文件夹", onLocalFolder)
            OptionRow(Icons.Filled.Search, "搜索 Apple Podcasts", onApple)
            OptionRow(Icons.Filled.Search, "搜索 fyyd", onFyyd)
            OptionRow(Icons.Filled.Search, "搜索播客索引", onIndex)
            OptionRow(AppIcons.Download, "导入播客列表（OPML）", onOpml)
        }
    }
}

@Composable
private fun OptionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(24.dp))
        Text(text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun RssDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean,
) {
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    CenterOverlay(onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)) {
                Text("通过 RSS 地址添加播客", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(20.dp))
                OutlinedInput(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    label = "RSS 地址",
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        if (onConfirm(text)) {
                            // handled by caller
                        } else {
                            error = "您输入的 RSS 地址无效。"
                        }
                    }) { Text("确认") }
                }
            }
        }
    }
}

@Composable
fun OutlinedInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    val focus = remember { FocusRequester() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        }
    }
}

@Composable
fun ProviderSearchScreen(
    provider: String,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                "返回",
                onBack,
                modifier = Modifier.size(40.dp),
            )
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("搜索", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        submitted = false
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitted = true }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(remember { FocusRequester() }),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "清除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { query = "" },
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            if (!submitted) {
                EmptyState("输入要搜索的查询")
            } else if (provider == "fyyd") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "java.io.IOException: unexpected end of stream on https://api.fyyd.de/...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("重试") }
                }
            } else {
                EmptyState("未找到\"$query\"的结果")
            }
        }
        Text(
            "按 $provider 显示结果",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 16.dp, bottom = 8.dp),
        )
    }
}

@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    showProviderHint: Boolean,
) {
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                "返回",
                onBack,
                modifier = Modifier.size(40.dp),
            )
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("搜索", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        submitted = false
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitted = true }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(remember { FocusRequester() }),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "清除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { query = "" },
                )
            }
        }
        if (submitted) {
            Row(Modifier.padding(start = 16.dp, top = 8.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        "在线搜索",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            if (!submitted) {
                EmptyState("输入要搜索的查询")
            } else {
                EmptyState("未找到\"$query\"的结果")
            }
        }
        if (submitted && showProviderHint) {
            Text(
                "按 Apple, Podcast Index 显示结果",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, bottom = 8.dp),
            )
        }
    }
}
