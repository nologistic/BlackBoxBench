package com.blackboxbench.reproduction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------- settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: FeederState, onBack: () -> Unit, onTextSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "back") }
                },
                title = { Text("设置") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingDropdown(
                title = "主题",
                subtitle = state.theme,
                options = listOf("系统", "浅色", "深色", "电子墨水"),
                selected = state.theme,
                onSelected = { state.theme = it; state.persist() },
            )
            SettingSwitch("动态配色", checked = state.dynamicColor) { state.dynamicColor = it; state.persist() }
            SettingDropdown(
                title = "首选深色主题",
                subtitle = state.darkTheme,
                options = listOf("黑色", "深灰", "深蓝"),
                selected = state.darkTheme,
                onSelected = { state.darkTheme = it; state.persist() },
            )
            SettingItem("新内容通知", "必须先授予权限才能启用通知。")

            SettingDivider()
            SettingSectionLabel("拦截列表")
            SettingItem("筛选条件")
            SettingSwitch("应用于摘要", checked = state.filterSummary) { state.filterSummary = it; state.persist() }
            SettingSwitch("应用于链接", checked = state.filterLinks) { state.filterLinks = it; state.persist() }

            SettingDivider()
            SettingSectionLabel("文本")
            SettingItem("文本设置", state.textFont, onClick = onTextSettings)

            SettingDivider()
            SettingSectionLabel("同步")
            SettingDropdown(
                title = "查找新文章…",
                subtitle = state.syncInterval,
                options = listOf("每 15 分钟", "每小时", "每 6 小时", "每天"),
                selected = state.syncInterval,
                onSelected = { state.syncInterval = it; state.persist() },
            )
            SettingSwitch("应用启动时", checked = state.syncOnStart) { state.syncOnStart = it; state.persist() }
            SettingSwitch("仅在连接 Wi-Fi 时", checked = state.syncWifiOnly) { state.syncWifiOnly = it; state.persist() }
            SettingSwitch("仅在充电时", checked = state.syncChargingOnly) { state.syncChargingOnly = it; state.persist() }
            SettingItem("每个订阅源的最大内容数", state.maxItems.toString())
            SettingItem("电池优化", "已开启")
            SettingItem("设备同步")

            SettingDivider()
            SettingSectionLabel("文章列表")
            SettingDropdown(
                title = "排序方式",
                subtitle = state.articleSort,
                options = listOf("从新到旧", "从旧到新"),
                selected = state.articleSort,
                onSelected = { state.articleSort = it; state.persist() },
            )
            SettingSwitch("显示悬浮操作按钮", checked = state.showFab) { state.showFab = it; state.persist() }
            SettingSwitch("标记所有为已读后显示订阅源", checked = state.showFeedsAfterMarkRead) {
                state.showFeedsAfterMarkRead = it; state.persist()
            }
            SettingDropdown(
                title = "文章样式",
                subtitle = state.articleStyle,
                options = listOf("卡片", "紧凑卡片", "紧凑", "超紧凑"),
                selected = state.articleStyle,
                onSelected = { state.articleStyle = it; state.persist() },
            )
            SettingItem("最大行数", state.maxLines.toString())
            SettingSwitch("仅显示标题", checked = state.titlesOnly) { state.titlesOnly = it; state.persist() }
            SettingDropdown(
                title = "滑动标记为已读",
                subtitle = state.swipeToRead,
                options = listOf("已禁用", "仅从右侧", "从任何方向"),
                selected = state.swipeToRead,
                onSelected = { state.swipeToRead = it; state.persist() },
            )
            SettingSwitch("滚动时自动标记为已读", checked = state.autoMarkRead) { state.autoMarkRead = it; state.persist() }
            SettingSwitch("显示缩略图", checked = state.showThumbnails) { state.showThumbnails = it; state.persist() }
            SettingSwitch("显示预估的阅读时间", checked = state.showReadingTime) { state.showReadingTime = it; state.persist() }
            SettingSwitch("在标题中显示未读文章数", checked = state.showUnreadCount) { state.showUnreadCount = it; state.persist() }
            SettingSwitch("强制单列布局", checked = state.forceSingleColumn) { state.forceSingleColumn = it; state.persist() }

            SettingDivider()
            SettingSectionLabel("阅读器")
            SettingDropdown(
                title = "内容默认打开方式",
                subtitle = state.contentOpenMode,
                options = listOf("阅读器", "自定义标签页", "默认浏览器"),
                selected = state.contentOpenMode,
                onSelected = { state.contentOpenMode = it; state.persist() },
            )
            SettingDropdown(
                title = "链接打开方式",
                subtitle = state.linkOpenMode,
                options = listOf("自定义标签页", "默认浏览器"),
                selected = state.linkOpenMode,
                onSelected = { state.linkOpenMode = it; state.persist() },
            )
            SettingSwitch("使用内置播放器播放音频链接", checked = state.builtInPlayer) { state.builtInPlayer = it; state.persist() }
            SettingSwitch("分页模式", subtitle = "通过“翻页”而非持续滚动来浏览文章。为 e-ink 设备优化。", checked = state.paginationMode) {
                state.paginationMode = it; state.persist()
            }

            SettingDivider()
            SettingSectionLabel("图片加载")
            SettingSwitch("仅在连接 Wi-Fi 时", checked = state.imagesWifiOnly) { state.imagesWifiOnly = it; state.persist() }

            SettingDivider()
            SettingSectionLabel("文本转语音")
            SettingSwitch("检测语言", checked = state.ttsDetectLanguage) { state.ttsDetectLanguage = it; state.persist() }

            SettingDivider()
            SettingSectionLabel("AI 和翻译")
            SettingItem("摘要 API", "未配置")
            SettingItem("翻译 API", "未配置")
            SettingItem("发送错误报告")

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun SettingSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
fun SettingItem(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingDropdown(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    onSelected(option)
                    expanded = false
                },
            )
        }
    }
}

// -------------------------------------------------------------- text settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSettingsScreen(state: FeederState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "back") }
                },
                title = { Text("文本设置") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("字体", style = MaterialTheme.typography.titleMedium)
            Text(state.textFont, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("文本缩放", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = state.textScale,
                    onValueChange = { state.textScale = it },
                    valueRange = 0.85f..1.3f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                Text("A", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
            Text("文本预览", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { }) { Text("粗体") }
                TextButton(onClick = { }) { Text("斜体", fontStyle = FontStyle.Italic) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The quick brown fox jumps over the lazy dog.",
                fontSize = (18 * state.textScale).sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "人生而自由",
                fontSize = (18 * state.textScale).sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "日本語の漢字ひらがなカタカナ",
                fontSize = (18 * state.textScale).sp,
            )
        }
    }
}

// --------------------------------------------------------------------- reader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: FeederState,
    articleId: String,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
) {
    val article = state.findArticle(articleId)
    if (article == null) {
        androidx.compose.runtime.LaunchedEffect(articleId) { onBack() }
        return
    }
    val feed = state.findFeed(article.feedId)
    val paragraphs = remember(articleId) { FeedParser.htmlToParagraphs(article.contentHtml) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "back") }
                },
                title = {
                    Text(
                        feed?.displayTitle ?: "",
                        maxLines = 1,
                    )
                },
                actions = {
                    IconButton(onClick = { state.toggleSaved(article) }) {
                        Icon(Icons.Default.Star, contentDescription = "save")
                    }
                    IconButton(onClick = { onToast("已分享链接") }) {
                        Icon(Icons.Default.Share, contentDescription = "share")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (article.image != null) {
                AssetImage(
                    name = article.image,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(article.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (feed?.displayTitle ?: "") + " · " + formatMillis(article.published),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                if (paragraphs.isEmpty()) {
                    Text("（无正文）")
                } else {
                    paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                if (article.link.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("原文链接", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        article.link,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))
