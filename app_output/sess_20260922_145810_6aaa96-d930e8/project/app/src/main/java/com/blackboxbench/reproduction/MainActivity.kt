package com.blackboxbench.reproduction

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        val prefs = Prefs(this)
        val state = AppState(this, store, prefs)
        val activity = this
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            val theme = prefs.str("theme", "System")
            LaunchedEffect(prefs.bool("noScreenshot", false), prefs.bool("keepScreenOn", false)) {
                if (prefs.bool("noScreenshot", false)) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                if (prefs.bool("keepScreenOn", false)) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            MarkorTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .background(LocalMarkorColors.current.background),
                    ) {
                        AppRoot(state)
                    }
                }
            }
        }
    }
}

private fun switchTab(nav: NavHostController, tab: String) {
    val route = when (tab) {
        "todo" -> "todo"
        "quicknote" -> "quicknote"
        "more" -> "more"
        else -> "browser"
    }
    runCatching {
        nav.navigate(route) {
            launchSingleTop = true
            popUpTo("browser") { inclusive = route == "browser" }
        }
    }
}

@Composable
private fun AppRoot(state: AppState) {
    val prefs = state.prefs
    val context = LocalContext.current
    val nav = rememberNavController()
    var showTour by remember { mutableStateOf(!prefs.bool("onboarded", false)) }
    var showGate by remember { mutableStateOf(false) }
    var gateHandled by remember { mutableStateOf(false) }

    val startRoute = remember {
        when (prefs.str("startTab", "笔记本")) {
            "To-Do" -> "todo"
            "QuickNote" -> "quicknote"
            else -> "browser"
        }
    }

    LaunchedEffect(showTour) {
        if (!showTour && !prefs.bool("storageGranted", false)) showGate = true
    }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(1800)
            state.message = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = startRoute) {
            composable("browser") {
                BrowserScreen(
                    state = state,
                    onOpenFile = { file ->
                        state.currentFile = file
                        nav.navigate("note")
                    },
                    onSelectTab = { switchTab(nav, it) },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenImport = { nav.navigate("import") },
                )
            }
            composable("note") {
                val file = state.currentFile
                if (file == null) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    NoteScreen(
                        state = state,
                        file = file,
                        showBottomBar = false,
                        selectedTab = "files",
                        onSelectTab = { switchTab(nav, it) },
                        onBack = { nav.popBackStack() },
                        onOpenBrowser = { nav.popBackStack() },
                    )
                }
            }
            composable("todo") {
                NoteScreen(
                    state = state,
                    file = state.store.todoFile(),
                    showBottomBar = true,
                    selectedTab = "todo",
                    onSelectTab = { switchTab(nav, it) },
                    onBack = { switchTab(nav, "files") },
                    onOpenBrowser = { switchTab(nav, "files") },
                )
            }
            composable("quicknote") {
                NoteScreen(
                    state = state,
                    file = state.store.quickNote(),
                    showBottomBar = true,
                    selectedTab = "quicknote",
                    onSelectTab = { switchTab(nav, it) },
                    onBack = { switchTab(nav, "files") },
                    onOpenBrowser = { switchTab(nav, "files") },
                )
            }
            composable("more") {
                MoreScreen(
                    state = state,
                    onSelectTab = { switchTab(nav, it) },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onOpenGeneral = { nav.navigate("settings/general") },
                    onOpenOther = { nav.navigate("settings/other") },
                    onOpenEdit = { nav.navigate("settings/edit") },
                    onOpenView = { nav.navigate("settings/view") },
                    onOpenFormat = { name ->
                        state.prefs.setStr("currentFormat", name)
                        nav.navigate("settings/format")
                    },
                )
            }
            composable("settings/general") { GeneralSettingsScreen(state) { nav.popBackStack() } }
            composable("settings/other") { OtherSettingsScreen(state) { nav.popBackStack() } }
            composable("settings/edit") { EditModeSettingsScreen(state) { nav.popBackStack() } }
            composable("settings/view") { ViewModeSettingsScreen(state) { nav.popBackStack() } }
            composable("settings/format") {
                FormatSettingsScreen(prefs.str("currentFormat", "Markdown"), state) { nav.popBackStack() }
            }
            composable("import") {
                ImportScreen(state) { nav.popBackStack() }
            }
        }

        ToastOverlay(state.message)

        if (showTour) {
            OnboardingTour(
                onFinish = {
                    prefs.setBool("onboarded", true)
                    showTour = false
                },
            )
        }

        if (showGate && !gateHandled) {
            AlertDialog(
                onDismissRequest = {
                    gateHandled = true
                    showGate = false
                },
                containerColor = LocalMarkorColors.current.surface,
                title = { Text("需要存储权限读写文件", color = LocalMarkorColors.current.textPrimary, fontSize = 20.sp) },
                text = null,
                confirmButton = {
                    TextButton(onClick = {
                        prefs.setBool("storageGranted", true)
                        gateHandled = true
                        showGate = false
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) { Text("确定", color = LocalMarkorColors.current.accent, fontSize = 16.sp) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        gateHandled = true
                        showGate = false
                        state.toast("未授予存储权限，使用应用内置笔记本")
                    }) { Text("退出", color = LocalMarkorColors.current.accent, fontSize = 16.sp) }
                },
            )
        }
    }
}

private val tourPages = listOf(
    "主界面" to "文件浏览器集中管理笔记本中的所有文档与文件夹",
    "查看" to "阅读模式渲染 Markdown 标记，随时切回编辑",
    "分享 -> Markor" to "从其他应用分享文本即可直接存为笔记",
    "To-Do" to "todo.txt 任务列表，支持优先级与完成标记",
    "QuickNote" to "随手记录，内容自动写入 QuickNote.md",
)

@Composable
private fun OnboardingTour(onFinish: () -> Unit) {
    val c = LocalMarkorColors.current
    var page by remember { mutableStateOf(0) }
    val last = page == tourPages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6101014))
            .clickable { if (last) onFinish() else page++ },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .background(Color(0xFF26252D), RoundedCornerShape(10.dp))
                .padding(24.dp),
        ) {
            Text(tourPages[page].first, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Text(tourPages[page].second, color = Color(0xFFCFCFD6), fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFF3A3944), RoundedCornerShape(6.dp)),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tourPages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == page) 10.dp else 8.dp)
                            .background(
                                if (index == page) c.accent else Color(0xFF6E6E78),
                                CircleShape,
                            ),
                    )
                }
            }
            if (last) {
                Text(
                    "完成",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { onFinish() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { page++ },
                    contentAlignment = Alignment.Center,
                ) { IcoArrowRight(size = 26.dp, tint = Color.White) }
            }
        }
    }
}
