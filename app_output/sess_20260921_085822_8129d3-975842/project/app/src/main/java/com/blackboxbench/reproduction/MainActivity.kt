package com.blackboxbench.reproduction

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF1E2038)
private val Accent = Color(0xFFF4494F)
private val Canvas = Color(0xFFF4F4F4)
private val Ink = Color(0xFF151515)
private val Muted = Color(0xFF6D6D6D)
private val Divider = Color(0xFFD9D9D9)

data class Doc(val name: String, val content: String, val favourite: Boolean = false, val folder: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(30, 32, 56)
        window.navigationBarColor = android.graphics.Color.rgb(30, 32, 56)
        setContent { BenchmarkAppTheme { MarkorApp() } }
    }
}

private fun loadDocs(context: Context): List<Doc> {
    val p = context.getSharedPreferences("markor", Context.MODE_PRIVATE)
    val names = p.getString("doc_names", "")!!.split("|").filter { it.isNotBlank() }
    val folders = p.getString("folders", "")!!.split("|").filter { it.isNotBlank() }.toSet()
    val favs = p.getString("favourites", "")!!.split("|").filter { it.isNotBlank() }.toSet()
    return (folders.map { Doc(it, "", false, true) } + names.map {
        Doc(it, p.getString("doc_$it", "") ?: "", favs.contains(it), false)
    }).sortedWith(compareByDescending<Doc> { it.folder }.thenBy { it.name.lowercase() })
}

private fun saveDocs(context: Context, docs: List<Doc>) {
    val p = context.getSharedPreferences("markor", Context.MODE_PRIVATE)
    val regular = docs.filterNot { it.folder }
    val folders = docs.filter { it.folder }
    p.edit()
        .putString("doc_names", regular.joinToString("|") { it.name })
        .putString("folders", folders.joinToString("|") { it.name })
        .putString("favourites", regular.filter { it.favourite }.joinToString("|") { it.name })
        .apply()
    regular.forEach { p.edit().putString("doc_${it.name}", it.content).apply() }
}

@Composable
fun MarkorApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("markor", Context.MODE_PRIVATE) }
    var onboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_done", false)) }
    var onboardingPage by remember { mutableIntStateOf(0) }
    var permissionDialog by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf("files") }
    var files by remember { mutableStateOf(loadDocs(context)) }
    var currentName by remember { mutableStateOf("") }
    var currentContent by remember { mutableStateOf("") }
    var currentFolder by remember { mutableStateOf("") }
    var quickNote by remember { mutableStateOf(prefs.getString("quicknote", "") ?: "") }
    var todo by remember { mutableStateOf(prefs.getString("todo", "") ?: "") }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }

    val openDoc: (Doc) -> Unit = { doc ->
        currentName = doc.name
        currentContent = doc.content
        screen = "editor"
    }
    val storeCurrent = {
        files = files.map { if (it.name == currentName) it.copy(content = currentContent) else it }
        saveDocs(context, files)
    }

    if (onboarding) {
        OnboardingScreen(
            page = onboardingPage,
            onNext = {
                if (onboardingPage < 4) onboardingPage++ else {
                    prefs.edit().putBoolean("onboarding_done", true).apply()
                    permissionDialog = true
                }
            }
        )
        if (permissionDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("需要存储权限读写文件", fontSize = 22.sp) },
                text = { Text("Markor 需要访问文档目录，以便创建、打开与保存文本文件。") },
                dismissButton = { TextButton(onClick = { permissionDialog = false; onboarding = false }) { Text("退出", color = Accent) } },
                confirmButton = { TextButton(onClick = { permissionDialog = false; onboarding = false }) { Text("确定", color = Accent) } }
            )
        }
        return
    }

    val bg = if (dark) Color(0xFF202124) else Canvas
    val fg = if (dark) Color(0xFFF1F1F1) else Ink

    when {
        screen == "files" -> FileBrowser(
            files = files,
            folder = currentFolder,
            bg = bg,
            fg = fg,
            onOpen = openDoc,
            onEnterFolder = { currentFolder = it },
            onLeaveFolder = { currentFolder = "" },
            onCreate = { name, type, isFolder ->
                val clean = name.ifBlank {
                    if (isFolder) "New folder" else SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US).format(Date())
                }
                if (isFolder) {
                    files = (files + Doc(clean, "", folder = true)).distinctBy { it.name }
                    saveDocs(context, files)
                } else {
                    val ext = when (type) {
                        "Plaintext" -> ".txt"; "todo.txt" -> ".txt"; "Wikitext/Zim" -> ".txt"
                        "AsciiDoc" -> ".adoc"; "OrgMode" -> ".org"; "CSV" -> ".csv"; else -> ".md"
                    }
                    val finalName = if (clean.endsWith(ext)) clean else clean + ext
                    val doc = Doc(finalName, "")
                    files = files + doc
                    saveDocs(context, files)
                    openDoc(doc)
                }
            },
            onFilesChange = {
                files = it
                saveDocs(context, files)
            },
            onBottom = { screen = it }
        )
        screen == "editor" -> EditorScreen(
            title = currentName.substringBeforeLast('.'),
            content = currentContent,
            bg = bg,
            fg = fg,
            onContent = { currentContent = it },
            onSave = storeCurrent,
            onBack = { storeCurrent(); screen = "files" }
        )
        screen == "todo" -> EditorScreen(
            title = "Markor",
            content = todo,
            bg = bg,
            fg = fg,
            todoMode = true,
            onContent = { todo = it },
            onSave = {
                prefs.edit().putString("todo", todo).apply()
                if (todo.isNotBlank() && files.none { it.name == "todo.txt" }) {
                    files = files + Doc("todo.txt", todo)
                    saveDocs(context, files)
                } else {
                    files = files.map { if (it.name == "todo.txt") it.copy(content = todo) else it }
                    saveDocs(context, files)
                }
            },
            onBack = { prefs.edit().putString("todo", todo).apply(); screen = "files" }
        )
        screen == "quick" -> EditorScreen(
            title = "QuickNote",
            content = quickNote,
            bg = bg,
            fg = fg,
            onContent = { quickNote = it },
            onSave = {
                prefs.edit().putString("quicknote", quickNote).apply()
                if (quickNote.isNotBlank() && files.none { it.name == "QuickNote.md" }) files = files + Doc("QuickNote.md", quickNote)
                else files = files.map { if (it.name == "QuickNote.md") it.copy(content = quickNote) else it }
                saveDocs(context, files)
            },
            onBack = { prefs.edit().putString("quicknote", quickNote).apply(); screen = "files" }
        )
        screen == "more" -> MoreScreen(bg, fg, onSettings = { screen = "settings" }, onBottom = { screen = it })
        screen == "settings" -> SettingsScreen(
            bg = bg, fg = fg, dark = dark,
            onDark = { dark = it; prefs.edit().putBoolean("dark", it).apply() },
            onBack = { screen = "more" }
        )
    }
}

@Composable
private fun OnboardingScreen(page: Int, onNext: () -> Unit) {
    val titles = listOf("主界面", "查看", "分享 → Markor", "To-Do", "QuickNote")
    val subtitles = listOf(
        "一个简洁的文本编辑器",
        "轻松阅读 Markdown 文档",
        "从其它应用直接分享文本到 Markor",
        "快速记录任务，并在完成后勾选",
        "随时捕捉转瞬即逝的想法"
    )
    val symbols = listOf("M", "◉", "↗", "✓", "ϟ")
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F7F7))) {
        Box(Modifier.fillMaxWidth().height(74.dp).background(Navy), contentAlignment = Alignment.CenterStart) {
            Text("Markor", color = Color.White, fontSize = 25.sp, modifier = Modifier.padding(start = 20.dp))
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(142.dp).background(Accent, CircleShape), contentAlignment = Alignment.Center) {
                Text(symbols[page], color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(48.dp))
            Text(titles[page], color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(20.dp))
            Text(subtitles[page], color = Muted, fontSize = 18.sp, textAlign = TextAlign.Center, lineHeight = 28.sp)
            Spacer(Modifier.height(60.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(5) { i -> Box(Modifier.size(if (i == page) 11.dp else 8.dp).background(if (i == page) Accent else Divider, CircleShape)) }
            }
        }
        Box(Modifier.fillMaxWidth().height(78.dp).clickable(onClick = onNext), contentAlignment = Alignment.CenterEnd) {
            Text(if (page == 4) "完成" else "›", color = Accent, fontSize = if (page == 4) 20.sp else 48.sp, modifier = Modifier.padding(end = 30.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileBrowser(
    files: List<Doc>, folder: String, bg: Color, fg: Color,
    onOpen: (Doc) -> Unit, onEnterFolder: (String) -> Unit, onLeaveFolder: () -> Unit,
    onCreate: (String, String, Boolean) -> Unit, onFilesChange: (List<Doc>) -> Unit,
    onBottom: (String) -> Unit
) {
    var createDialog by remember { mutableStateOf(false) }
    var searchDialog by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var importDialog by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Doc?>(null) }
    var info by remember { mutableStateOf<Doc?>(null) }
    var rename by remember { mutableStateOf<Doc?>(null) }
    var delete by remember { mutableStateOf<Doc?>(null) }

    BackHandler(enabled = folder.isNotBlank() || selected != null) {
        if (selected != null) selected = null else onLeaveFolder()
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            Column {
                Box(Modifier.fillMaxWidth().height(72.dp).background(Navy)) {
                    if (selected == null) {
                        Text(if (folder.isBlank()) "Markor" else "> $folder", color = Color.White, fontSize = 28.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
                        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                            HeaderButton("▰") { importDialog = true }
                            HeaderButton("≡") { sortDialog = true }
                            HeaderButton("⌕") { searchDialog = true }
                            Box { HeaderButton("⋮") { menu = true }; DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("从本设备导入") }, onClick = { menu = false; importDialog = true })
                                DropdownMenuItem(text = { Text("设置") }, onClick = { menu = false; onBottom("settings") })
                            } }
                        }
                    } else {
                        Column(Modifier.align(Alignment.CenterStart).padding(start = 18.dp)) {
                            Text("Markor", color = Color.White, fontSize = 24.sp)
                            Text("(1 / ${files.size})", color = Color.LightGray, fontSize = 17.sp)
                        }
                        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                            HeaderButton(if (selected!!.favourite) "★" else "☆") {
                                val d = selected!!
                                onFilesChange(files.map { if (it.name == d.name) it.copy(favourite = !it.favourite) else it })
                                selected = d.copy(favourite = !d.favourite)
                            }
                            HeaderButton("A|") { rename = selected }
                            HeaderButton("ⓘ") { info = selected }
                            HeaderButton("▣") { delete = selected }
                            HeaderButton("⋮") {}
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(58.dp).background(bg).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("▰", color = Muted, fontSize = 34.sp)
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text("..", color = fg, fontSize = 20.sp)
                        Text(if (folder.isBlank()) "/storage/emulated/0/Documents" else "/storage/emulated/0/Documents/$folder", color = Muted, fontSize = 17.sp)
                    }
                }
                HorizontalDivider(color = Divider)
            }
        },
        bottomBar = { BottomNavigation("files", onBottom) },
        floatingActionButton = {
            FloatingActionButton(onClick = { createDialog = true }, shape = RoundedCornerShape(24.dp), containerColor = Accent, contentColor = Color.White, modifier = Modifier.size(70.dp)) {
                Text("+", fontSize = 38.sp, fontWeight = FontWeight.Light)
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().background(bg)) {
            if (folder.isNotBlank()) {
                FileRow(Doc("..", "", folder = true), fg, onClick = onLeaveFolder, onLong = {})
            }
            val shown = if (folder.isBlank()) files else emptyList()
            if (shown.isEmpty() && folder.isBlank()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("¯\\_(ツ)_/¯", color = Color(0xFF999999), fontSize = 34.sp)
                    Text("这里还没有文件", color = Muted, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
                }
            } else {
                shown.forEach { doc ->
                    FileRow(doc, fg, onClick = { if (doc.folder) onEnterFolder(doc.name) else onOpen(doc) }, onLong = { selected = doc }, selected = selected?.name == doc.name)
                }
            }
        }
    }

    if (createDialog) NewDocumentDialog(onDismiss = { createDialog = false }, onCreate = { n, t, f -> createDialog = false; onCreate(n, t, f) })
    if (searchDialog) SearchDialog(files.filterNot { it.folder }, onDismiss = { searchDialog = false }, onOpen = { searchDialog = false; onOpen(it) })
    if (sortDialog) SortDialog { sortDialog = false }
    if (importDialog) ImportDialog { importDialog = false }
    info?.let { FileInfoDialog(it) { info = null } }
    rename?.let { old ->
        RenameDialog(old.name, onDismiss = { rename = null }, onConfirm = { new ->
            onFilesChange(files.map { if (it.name == old.name) it.copy(name = new) else it }); selected = null; rename = null
        })
    }
    delete?.let { d ->
        AlertDialog(
            onDismissRequest = { delete = null },
            title = { Text("确认删除", fontSize = 24.sp) },
            text = { Text(d.name, fontSize = 18.sp) },
            dismissButton = { TextButton(onClick = { delete = null }) { Text("取消", color = Accent) } },
            confirmButton = { TextButton(onClick = { onFilesChange(files.filterNot { it.name == d.name }); selected = null; delete = null }) { Text("确定", color = Accent) } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(doc: Doc, fg: Color, onClick: () -> Unit, onLong: () -> Unit, selected: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().height(78.dp)
            .background(if (selected) Color(0xFFE8E8E8) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(when { selected -> "✓"; doc.folder -> "▰"; doc.name == "QuickNote.md" -> "ϟ"; doc.name == "todo.txt" -> "☑"; else -> "▱" },
            color = when { selected -> Accent; doc.favourite -> Color(0xFFE4B900); else -> Color(0xFF858585) }, fontSize = 34.sp)
        Spacer(Modifier.width(20.dp))
        Column {
            Text(doc.name, color = fg, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            if (doc.name != "..") Text(SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA).format(Date()), color = Muted, fontSize = 17.sp)
        }
    }
    HorizontalDivider(color = Divider)
}

@Composable
private fun HeaderButton(label: String, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = if (label == "⋮") 31.sp else 27.sp)
    }
}

@Composable
private fun BottomNavigation(current: String, onBottom: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(88.dp).background(Navy), verticalAlignment = Alignment.CenterVertically) {
        listOf(
            Triple("files", "▰", "文件"), Triple("todo", "☑", "To-Do"),
            Triple("quick", "ϟ", "QuickNote"), Triple("more", "♥", "更多")
        ).forEach { (id, icon, label) ->
            Column(Modifier.weight(1f).fillMaxHeight().clickable { onBottom(id) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(icon, color = if (current == id) Accent else Color.White, fontSize = 27.sp)
                Text(label, color = if (current == id) Accent else Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun NewDocumentDialog(onDismiss: () -> Unit, onCreate: (String, String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Markdown") }
    var template by remember { mutableStateOf("Empty") }
    var typeMenu by remember { mutableStateOf(false) }
    var templateMenu by remember { mutableStateOf(false) }
    val ext = when (type) { "Plaintext", "todo.txt", "Wikitext/Zim" -> ".txt"; "AsciiDoc" -> ".adoc"; "OrgMode" -> ".org"; "CSV" -> ".csv"; else -> ".md" }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFAFAFA), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicTextField(value = name, onValueChange = { name = it }, singleLine = true,
                        textStyle = TextStyle(fontSize = 22.sp, color = Ink), cursorBrush = SolidColor(Accent),
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                    Text(ext, color = Muted, fontSize = 20.sp)
                }
                HorizontalDivider(color = Accent)
                Spacer(Modifier.height(18.dp))
                Text("格式", color = Muted, fontSize = 14.sp)
                Text("{{title}}", color = Ink, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Box {
                    Column(Modifier.fillMaxWidth().clickable { typeMenu = true }.padding(vertical = 7.dp)) {
                        Text("文件类型", color = Muted, fontSize = 13.sp); Text(type, color = Ink, fontSize = 18.sp)
                    }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        listOf("Markdown","Plaintext","todo.txt","Wikitext/Zim","AsciiDoc","OrgMode","CSV").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { type = it; typeMenu = false })
                        }
                    }
                }
                Box {
                    Column(Modifier.fillMaxWidth().clickable { templateMenu = true }.padding(vertical = 7.dp)) {
                        Text("模板", color = Muted, fontSize = 13.sp); Text(template, color = Ink, fontSize = 18.sp)
                    }
                    DropdownMenu(expanded = templateMenu, onDismissRequest = { templateMenu = false }) {
                        listOf("Empty","asciidoc-reference.adoc","cooking-recipe.md","hugo-post-front-matter.md","jekyll-post.adoc","jekyll-post.md","markor-markdown-reference.md","orgmode-reference.org","presentation-beamer.md","sample.csv","todo.example.txt","zettelkasten.md").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { template = it; templateMenu = false })
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onCreate(name, type, true) }) { Text("Folder", color = Accent) }
                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = Accent) }
                        TextButton(onClick = { onCreate(name, type, false) }) { Text("OK", color = Accent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchDialog(files: List<Doc>, onDismiss: () -> Unit, onOpen: (Doc) -> Unit) {
    var query by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<Doc>?>(null) }
    if (results != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择搜索结果") },
            text = {
                Column {
                    results!!.forEach { d ->
                        Column(Modifier.fillMaxWidth().clickable { onOpen(d) }.padding(vertical = 10.dp)) {
                            Text(d.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val hit = d.content.lineSequence().firstOrNull { it.contains(query, true) }
                            if (hit != null) Text("+0: $hit", color = Muted)
                        }
                    }
                    if (results!!.isEmpty()) Text("没有找到匹配结果", color = Muted)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("关闭", color = Accent) } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("搜索", fontSize = 24.sp) },
            text = {
                Column {
                    Text("递归搜索当前目录", color = Muted)
                    BasicTextField(query, { query = it }, textStyle = TextStyle(fontSize = 20.sp, color = Ink),
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp))
                    HorizontalDivider(color = Accent)
                    CheckLine("正则表达式", false) {}
                    CheckLine("区分大小写", false) {}
                    CheckLine("搜索文件内容", content) { content = it }
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Accent) } },
            confirmButton = { TextButton(onClick = { results = files.filter { it.name.contains(query, true) || (content && it.content.contains(query, true)) } }) { Text("确定", color = Accent) } }
        )
    }
}

@Composable
private fun SortDialog(onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf("名称") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序") },
        text = {
            Column {
                CheckLine("显示本地文件夹", true) {}
                listOf("名称","日期","大小","MIME type").forEach { label ->
                    Row(Modifier.fillMaxWidth().clickable { selected = label }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected == label, onClick = { selected = label }, colors = RadioButtonDefaults.colors(selectedColor = Accent))
                        Text(label)
                    }
                }
                CheckLine("文件夹优先", true) {}
                CheckLine("反向排序", false) {}
                CheckLine("显示点文件", false) {}
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定", color = Accent) } }
    )
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(620.dp)) {
            Column {
                Text("从本设备导入", fontSize = 24.sp, modifier = Modifier.padding(22.dp))
                Text("/storage/emulated/0", color = Muted, modifier = Modifier.padding(horizontal = 22.dp))
                HorizontalDivider()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    listOf("Alarms","Android","Audiobooks","DCIM","Documents","Download","Movies","Music","Notifications","Pictures","Podcasts","Recordings").forEach {
                        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("▰", color = Muted, fontSize = 24.sp); Spacer(Modifier.width(16.dp)); Text(it, fontSize = 18.sp)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(10.dp)) { Text("取消", color = Accent) }
            }
        }
    }
}

@Composable
private fun FileInfoDialog(doc: Doc, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("信息", fontSize = 25.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                InfoField("名称", doc.name)
                InfoField("位置", "/storage/emulated/0/Documents/markor")
                InfoField("上次修改:", SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA).format(Date()))
                InfoField("按文件大小排序", "${doc.content.toByteArray().size}B")
                InfoField("MIME type", if (doc.name.endsWith(".md")) "text/markdown" else "text/plain")
                val lines = if (doc.content.isEmpty()) 0 else doc.content.lines().size
                InfoField("文本行数 / 文字词 / 文字符号", "$lines / ${doc.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size} / ${doc.content.length}")
                Text("设置", fontSize = 24.sp, modifier = Modifier.padding(top = 14.dp))
                CheckLine("最近文件列表", true) {}
                CheckLine("收藏", doc.favourite) {}
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定", color = Accent) } }
    )
}

@Composable private fun InfoField(label: String, value: String) {
    Text(label, color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 9.dp))
    Text(value, fontSize = 18.sp)
}

@Composable
private fun RenameDialog(old: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(old) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("重命名") },
        text = { BasicTextField(value, { value = it }, textStyle = TextStyle(fontSize = 20.sp, color = Ink), modifier = Modifier.fillMaxWidth().border(0.dp, Color.Transparent)) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Accent) } },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("确定", color = Accent) } })
}

@Composable
private fun EditorScreen(
    title: String, content: String, bg: Color, fg: Color, todoMode: Boolean = false,
    onContent: (String) -> Unit, onSave: () -> Unit, onBack: () -> Unit
) {
    var preview by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }
    var tools by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf(false) }
    var lineNumbers by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(true) }

    BackHandler {
        if (preview) preview = false else { onSave(); onBack() }
    }

    Column(Modifier.fillMaxSize().background(bg)) {
        Box(Modifier.fillMaxWidth().height(72.dp).background(Navy)) {
            Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
            Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                HeaderButton("↶") {}
                HeaderButton("↷") {}
                HeaderButton(if (saved) "▣" else "▰") { onSave(); saved = true }
                HeaderButton(if (preview) "✎" else "◉") { preview = !preview }
                HeaderButton("⌕") { search = true }
                Box {
                    HeaderButton("⋮") { overflow = true }
                    DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                        DropdownMenuItem(text = { Text("文件设置") }, onClick = { overflow = false; settings = true })
                        DropdownMenuItem(text = { Text("分享") }, onClick = { overflow = false; share = true })
                        DropdownMenuItem(text = { Text("工具") }, onClick = { overflow = false; tools = true })
                        DropdownMenuItem(text = { Text("文件浏览器") }, onClick = { overflow = false; onSave(); onBack() })
                        DropdownMenuItem(text = { Text("信息") }, onClick = { overflow = false; info = true })
                        DropdownMenuItem(text = { Text("刷新") }, onClick = { overflow = false })
                    }
                }
            }
        }
        if (preview) {
            PreviewPane(content, fg, Modifier.weight(1f).fillMaxWidth().padding(20.dp))
        } else {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (lineNumbers) {
                    Text((1..maxOf(1, content.lines().size)).joinToString("\n"), color = Color(0xFF999999), fontSize = 18.sp, lineHeight = 26.sp, textAlign = TextAlign.End, modifier = Modifier.width(38.dp).padding(top = 14.dp, end = 6.dp))
                }
                BasicTextField(
                    value = content,
                    onValueChange = { onContent(it); saved = false },
                    textStyle = TextStyle(color = fg, fontSize = 19.sp, lineHeight = 26.sp),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(14.dp)
                )
            }
        }
        EditorToolbar(todoMode = todoMode, onInsert = { token ->
            val next = if (content.isBlank()) token else content + token
            onContent(next); saved = false
        })
    }

    if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text("文件设置") }, text = {
        Column {
            CheckLine("自动换行", true) {}
            CheckLine("显示行号", lineNumbers) { lineNumbers = it }
            CheckLine("语法高亮", true) {}
            CheckLine("自动格式化", true) {}
            InfoField("字体", "Roboto Regular")
        }
    }, confirmButton = { TextButton(onClick = { settings = false }) { Text("确定", color = Accent) } })

    if (share) AlertDialog(onDismissRequest = { share = false }, title = { Text("分享") }, text = {
        Column { listOf("URL / 路径","纯文本","打印 / PDF","文件","HTML","HTML（显示为纯文本）","图片","截图","日历").forEach { Text(it, fontSize = 18.sp, modifier = Modifier.fillMaxWidth().clickable { share = false }.padding(vertical = 9.dp)) } }
    }, confirmButton = { TextButton(onClick = { share = false }) { Text("取消", color = Accent) } })

    if (tools) AlertDialog(onDismissRequest = { tools = false }, title = { Text("工具") }, text = {
        Text("Speed Read", fontSize = 20.sp, modifier = Modifier.fillMaxWidth().clickable { tools = false; speed = true }.padding(vertical = 14.dp))
    }, confirmButton = {})

    if (speed) AlertDialog(onDismissRequest = { speed = false }, title = { Text("+100 WORDS (CLICK)\n-100 LONG CLICK SWIPE DOWN", fontSize = 15.sp) }, text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().height(240.dp), verticalArrangement = Arrangement.Center) {
            Text("Word: ${content.split(Regex("\\s+")).filter { it.isNotBlank() }.size}", color = Muted)
            Text(content.split(Regex("\\s+")).lastOrNull { it.isNotBlank() } ?: "Markor", fontSize = 48.sp, fontWeight = FontWeight.Light)
        }
    }, confirmButton = { TextButton(onClick = { speed = false }) { Text("取消", color = Accent) } })

    if (info) AlertDialog(onDismissRequest = { info = false }, title = { Text("信息") }, text = {
        Column { InfoField("名称", title); InfoField("文本行数 / 文字词 / 文字符号", "${if(content.isEmpty()) 0 else content.lines().size} / ${content.split(Regex("\\s+")).filter { it.isNotBlank() }.size} / ${content.length}") }
    }, confirmButton = { TextButton(onClick = { info = false }) { Text("确定", color = Accent) } })

    if (search) {
        var query by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { search = false }, title = { Text("在文档中查找") }, text = {
            Column { BasicTextField(query, { query = it }, textStyle = TextStyle(fontSize = 20.sp, color = Ink)); HorizontalDivider(color = Accent); if (query.isNotBlank()) Text(if (content.contains(query, true)) "找到匹配内容" else "没有匹配内容", modifier = Modifier.padding(top = 16.dp)) }
        }, confirmButton = { TextButton(onClick = { search = false }) { Text("完成", color = Accent) } })
    }
}

@Composable
private fun PreviewPane(content: String, fg: Color, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        if (content.isBlank()) return@Column
        content.lines().forEach { line ->
            when {
                line.startsWith("# ") -> Text(line.removePrefix("# "), color = fg, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                line.startsWith("## ") -> Text(line.removePrefix("## "), color = fg, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 7.dp))
                line.startsWith("- [x]", true) -> Text("☑  " + line.drop(5).trim(), color = Muted, fontSize = 19.sp, modifier = Modifier.padding(vertical = 5.dp))
                line.startsWith("- [ ]") -> Text("☐  " + line.drop(5).trim(), color = fg, fontSize = 19.sp, modifier = Modifier.padding(vertical = 5.dp))
                line.startsWith("- ") -> Text("•  " + line.drop(2), color = fg, fontSize = 19.sp, modifier = Modifier.padding(vertical = 5.dp))
                else -> Text(line.replace("**",""), color = fg, fontSize = 19.sp, lineHeight = 28.sp, modifier = Modifier.padding(vertical = 5.dp))
            }
        }
    }
}

@Composable
private fun EditorToolbar(todoMode: Boolean, onInsert: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(72.dp).background(Color(0xFFF8F8F8)).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        val buttons = if (todoMode) listOf("☑" to "\n- [ ] ", "↶" to "", "◷" to " @17:00", "⌨" to "", "•≡" to "\n- ", "<>" to "``")
        else listOf("1≡" to "\n1. ", "↶" to "", "◷" to "", "⌨" to "", "•≡" to "\n- ", "<>" to "``", "B" to "****", "H2" to "\n## ", "H3" to "\n### ", "↗" to "[]()")
        buttons.forEach { (label, token) ->
            Box(Modifier.width(72.dp).fillMaxHeight().clickable { if (token.isNotEmpty()) onInsert(token) }, contentAlignment = Alignment.Center) {
                Text(label, color = Ink, fontSize = if (label.length > 2) 19.sp else 26.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MoreScreen(bg: Color, fg: Color, onSettings: () -> Unit, onBottom: (String) -> Unit) {
    Scaffold(containerColor = bg, topBar = {
        Box(Modifier.fillMaxWidth().height(72.dp).background(Navy), contentAlignment = Alignment.CenterStart) {
            Text("更多", color = Color.White, fontSize = 28.sp, modifier = Modifier.padding(start = 18.dp))
        }
    }, bottomBar = { BottomNavigation("more", onBottom) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp).background(Accent, CircleShape), contentAlignment = Alignment.Center) { Text("M", color = Color.White, fontSize = 46.sp) }
                Text("Markor", color = fg, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text("net.gsantner.markor", color = Muted)
                Text("Version v2.16.1 (163)", color = Muted)
            }
            MoreItem("反馈 / 问题", "报告问题或提出建议", fg) {}
            MoreItem("设置", "配置编辑器、视图和格式", fg, onSettings)
            MoreItem("帮助 / FAQ", "使用说明与常见问题", fg) {}
            MoreItem("为应用评分", "如果喜欢 Markor，请支持我们", fg) {}
            SectionTitle("项目团队")
            MoreItem("Gregor Santner", "项目维护者", fg) {}
            SectionTitle("社区")
            MoreItem("翻译", "帮助翻译 Markor", fg) {}
            MoreItem("加入社区", "参与讨论", fg) {}
            MoreItem("贡献者", "感谢所有贡献者", fg) {}
            SectionTitle("开源")
            MoreItem("Apache License 2.0", "开源许可证", fg) {}
            MoreItem("许可证", "查看第三方许可证", fg) {}
            MoreItem("源代码", "查看项目源代码", fg) {}
            MoreItem("构建信息", "版本与构建详情", fg) {}
        }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, color = Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 22.dp, bottom = 6.dp)) }
@Composable private fun MoreItem(title: String, subtitle: String, fg: Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp)) {
        Text(title, color = fg, fontSize = 20.sp)
        Text(subtitle, color = Muted, fontSize = 14.sp)
    }
    HorizontalDivider(color = Divider)
}

@Composable
private fun SettingsScreen(bg: Color, fg: Color, dark: Boolean, onDark: (Boolean) -> Unit, onBack: () -> Unit) {
    var sub by remember { mutableStateOf("") }
    var themeDialog by remember { mutableStateOf(false) }
    BackHandler { if (sub.isNotBlank()) sub = "" else onBack() }
    Column(Modifier.fillMaxSize().background(bg)) {
        Row(Modifier.fillMaxWidth().height(72.dp).background(Navy), verticalAlignment = Alignment.CenterVertically) {
            HeaderButton("‹") { if (sub.isNotBlank()) sub = "" else onBack() }
            Text(if (sub.isBlank()) "设置" else sub, color = Color.White, fontSize = 26.sp)
        }
        if (sub.isBlank()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                SectionTitle("设置")
                SettingsItem("通用", "保存路径、启动页、搜索", fg) { sub = "通用" }
                SettingsItem("其它", "导航栏、分享与备份", fg) { sub = "其它" }
                SettingsItem("编辑模式", "字体、语法与快捷按钮", fg) { sub = "编辑模式" }
                SettingsItem("视图模式", "渲染、链接与字体", fg) { sub = "视图模式" }
                SectionTitle("格式")
                SettingsItem("Markdown", "Markdown 专属设置", fg) { sub = "Markdown" }
                SettingsItem("todo.txt", "完成日期、上下文与项目", fg) { sub = "todo.txt" }
                SettingsItem("Wikitext / Zim", "维基文本设置", fg) { sub = "Wikitext / Zim" }
                SettingsItem("Plaintext", "纯文本设置", fg) { sub = "Plaintext" }
                SettingsItem("AsciiDoc", "AsciiDoc 设置", fg) { sub = "AsciiDoc" }
                SettingsItem("OrgMode", "OrgMode 设置", fg) { sub = "OrgMode" }
                SectionTitle("基础设置")
                SettingsItem("主题", if (dark) "Dark" else "System", fg) { themeDialog = true }
                SettingsItem("语言", "中文（中国） · 需要重启", fg) {}
            }
        } else {
            SettingsSubPage(sub, fg)
        }
    }
    if (themeDialog) {
        var selection by remember { mutableStateOf(if (dark) "Dark" else "System") }
        AlertDialog(onDismissRequest = { themeDialog = false }, title = { Text("主题") }, text = {
            Column { listOf("System","Auto","Auto (09:00-17:00)","Light","Dark","Black").forEach { item ->
                Row(Modifier.fillMaxWidth().clickable { selection = item; onDark(item == "Dark" || item == "Black"); themeDialog = false }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selection == item, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Accent)); Text(item)
                }
            } }
        }, confirmButton = {})
    }
}

@Composable private fun SettingsItem(title: String, subtitle: String, fg: Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp)) {
        Text(title, color = fg, fontSize = 20.sp)
        Text(subtitle, color = Muted, fontSize = 14.sp)
    }
    HorizontalDivider(color = Divider)
}

@Composable
private fun SettingsSubPage(kind: String, fg: Color) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        when (kind) {
            "通用" -> {
                SectionTitle("保存路径")
                SettingsItem("笔记本", "Documents/markor", fg) {}
                SettingsItem("QuickNote", "Documents/markor/QuickNote.md", fg) {}
                SettingsItem("To-Do", "Documents/markor/todo.txt", fg) {}
                SectionTitle("启动")
                SettingsItem("启动标签", "笔记本", fg) {}
                SettingsItem("起始文件夹", "笔记本", fg) {}
                SectionTitle("搜索")
                SettingsItem("搜索深度", "无限", fg) {}
                SettingsItem("忽略列表", "", fg) {}
                SectionTitle("功能")
                CheckLine("语法高亮", true) {}
                CheckLine("Launcher 特殊文件", false) {}
                CheckLine("多窗口", false) {}
                CheckLine("实验性功能", false) {}
            }
            "其它" -> {
                SectionTitle("界面")
                SettingsItem("导航栏颜色", "黑色", fg) {}
                CheckLine("保持屏幕常亮", false) {}
                CheckLine("禁止截图", false) {}
                SectionTitle("分享")
                SettingsItem("分享格式", "默认", fg) {}
                SettingsItem("附件文件夹名称", "attachments", fg) {}
                SettingsItem("片段 / 模板目录", "/storage/emulated/0/Documents/markor/.app/snippets", fg) {}
                CheckLine("Chrome 自定义标签页", true) {}
                SectionTitle("备份设置")
                SettingsItem("导出设置", "JSON", fg) {}
                SettingsItem("恢复设置", "导入 JSON 并重启", fg) {}
            }
            "编辑模式" -> {
                SettingsItem("字体", "Roboto Regular", fg) {}
                Text("字体大小   15", color = fg, fontSize = 18.sp, modifier = Modifier.padding(20.dp))
                Slider(value = .45f, onValueChange = {}, colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent), modifier = Modifier.padding(horizontal = 20.dp))
                SectionTitle("语法")
                CheckLine("十六进制颜色下划线", false) {}
                CheckLine("禁用语法检查", true) {}
                CheckLine("等宽代码", false) {}
                CheckLine("禁用代码块高亮", false) {}
                CheckLine("大标题", false) {}
                SectionTitle("编辑")
                CheckLine("自动换行", true) {}
                CheckLine("文本居中", false) {}
                CheckLine("在末尾开始", false) {}
                CheckLine("Tab 缩进", true) {}
            }
            "视图模式" -> {
                CheckLine("首选视图模式", false) {}
                SettingsItem("注入到 <head>", "", fg) {}
                SettingsItem("注入到 <body>", "", fg) {}
                SettingsItem("链接颜色", "绿色", fg) {}
                CheckLine("从右向左", false) {}
                SettingsItem("字体大小", "-1（使用编辑器设置）", fg) {}
            }
            "Markdown" -> {
                CheckLine("高亮行尾", false) {}
                CheckLine("换行视为新段落", false) {}
                CheckLine("渲染 KaTeX", false) {}
                SettingsItem("目录", "TOC", fg) {}
                SettingsItem("Front Matter", "启用", fg) {}
                SettingsItem("操作按钮顺序", "默认", fg) {}
            }
            "todo.txt" -> {
                CheckLine("添加完成日期", true) {}
                CheckLine("追加上下文 / 项目", true) {}
                SettingsItem("操作按钮顺序", "默认", fg) {}
                SettingsItem("其它上下文 / 项目", "@context  +project", fg) {}
                SettingsItem("语法标题", "todo.txt", fg) {}
            }
            else -> {
                SectionTitle(kind)
                SettingsItem("文件扩展名", "默认", fg) {}
                CheckLine("语法高亮", true) {}
            }
        }
    }
}

@Composable
private fun CheckLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange, colors = CheckboxDefaults.colors(checkedColor = Accent))
        Text(label, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
