package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent { BenchmarkAppTheme { ReproducedApp() } }
    }
}

@Composable
fun ReproducedApp() {
    val nav = rememberNavController()
    var drawerOpen by remember { mutableStateOf(false) }
    val start = if (Store.onboarded) "list/${Store.lastViewId}" else "onboarding"

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        NavHost(navController = nav, startDestination = start) {
            composable("onboarding") {
                OnboardingScreen(
                    onAddAccount = { nav.navigate("addaccount") },
                    onSkip = {
                        Store.onboarded = true
                        Store.lastViewId = "mytasks"
                        Store.currentViewId = "mytasks"
                        Store.save()
                        nav.navigate("list/mytasks") { popUpTo("onboarding") { inclusive = true } }
                    }
                )
            }
            composable("addaccount") {
                AddAccountScreen(onBack = { nav.popBackStack() })
            }
            composable(
                "list/{viewId}",
                arguments = listOf(navArgument("viewId") { type = NavType.StringType })
            ) { entry ->
                val viewId = entry.arguments?.getString("viewId") ?: "mytasks"
                Store.currentViewId = viewId
                Store.lastViewId = viewId
                TaskListScreen(
                    viewId = viewId,
                    onOpenDrawer = { drawerOpen = true },
                    onOpenEditor = { taskId -> nav.navigate("editor?taskId=$taskId&base=$viewId") },
                    onNewTask = { nav.navigate("editor?taskId=&base=$viewId") },
                    onAppSettings = { nav.navigate("settings") },
                    onViewSettings = { id ->
                        nav.navigate("newgroup/${if (id.startsWith("list_")) "list" else "tag"}?id=$id")
                    },
                )
            }
            composable(
                "editor?taskId={taskId}&base={base}",
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("base") { type = NavType.StringType; defaultValue = "mytasks" },
                )
            ) { entry ->
                val taskId = entry.arguments?.getString("taskId").orEmpty().ifBlank { null }
                val base = entry.arguments?.getString("base") ?: "mytasks"
                TaskEditorScreen(taskId = taskId, baseViewId = base, onClose = { nav.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { page -> nav.navigate("settingspage/$page") }
                )
            }
            composable(
                "settingspage/{page}",
                arguments = listOf(navArgument("page") { type = NavType.StringType })
            ) { entry ->
                val page = entry.arguments?.getString("page") ?: "about"
                SettingsPage(page, onBack = { nav.popBackStack() })
            }
            composable(
                "newgroup/{kind}?id={id}",
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType; defaultValue = "" },
                )
            ) { entry ->
                val kind = entry.arguments?.getString("kind") ?: "list"
                val id = entry.arguments?.getString("id").orEmpty().ifBlank { null }
                GroupEditorScreen(kind = kind, existingId = id, onClose = { nav.popBackStack() })
            }
            composable("filtereditor") {
                FilterEditorScreen(onClose = { nav.popBackStack() })
            }
        }

        if (drawerOpen) {
            BackHandler { drawerOpen = false }
            Box(
                Modifier.fillMaxSize().background(Color(0x73000000))
                    .clickable { drawerOpen = false }
            )
            Box(Modifier.align(Alignment.CenterStart).fillMaxHeight()) {
                AppDrawer(
                    currentView = Store.currentViewId,
                    onSelect = { viewId ->
                        drawerOpen = false
                        Store.currentViewId = viewId
                        Store.lastViewId = viewId
                        Store.save()
                        nav.navigate("list/$viewId") { launchSingleTop = true }
                    },
                    onAddFilter = { drawerOpen = false; nav.navigate("filtereditor") },
                    onAddTag = { drawerOpen = false; nav.navigate("newgroup/tag") },
                    onAddList = { drawerOpen = false; nav.navigate("newgroup/list") },
                    onSettings = { drawerOpen = false; nav.navigate("settings") },
                    onSearch = { drawerOpen = false },
                )
            }
        }
    }
}

@Composable
private fun SettingsPage(page: String, onBack: () -> Unit) {
    when (page) {
        "appearance" -> AppearanceScreen(onBack)
        "accounts" -> AddAccountScreen(onBack)
        "locallists" -> SimpleSettingsScreen(
            "本地清单",
            Store.lists.map { it.name to Store.countsFor(it.id).toString() },
            onBack
        )
        "donate" -> SimpleSettingsScreen("捐赠", listOf("考虑用捐赠显示您的支持！" to ""), onBack)
        "notifications" -> SimpleSettingsScreen(
            "通知", listOf("提醒通知" to "已启用", "通知声音" to "默认"), onBack
        )
        "defaults" -> SimpleSettingsScreen(
            "任务默认值",
            listOf("默认清单" to (Store.lists.firstOrNull()?.name ?: ""), "默认优先级" to "无"),
            onBack
        )
        "listoptions" -> SimpleSettingsScreen(
            "任务清单选项",
            listOf("显示已完成任务" to if (Store.showCompleted) "开" else "关", "分组" to "按截止日期"),
            onBack
        )
        "editoptions" -> SimpleSettingsScreen(
            "编辑屏幕选项", listOf("显示计时器" to "开", "显示日历" to "开"), onBack
        )
        "datetime" -> SimpleSettingsScreen(
            "日期和时间", listOf("使用 24 小时制" to "开", "一周的第一天" to "周一"), onBack
        )
        "drawer" -> SimpleSettingsScreen(
            "导航抽屉", listOf("显示我的任务" to "开", "显示过滤器" to "开", "显示标签" to "开"), onBack
        )
        "backup" -> SimpleSettingsScreen(
            "备份", listOf("自动备份" to "已关闭", "立即备份" to ""), onBack
        )
        "widgets" -> SimpleSettingsScreen("插件设置", listOf("小部件主题" to "跟随应用"), onBack)
        "advanced" -> SimpleSettingsScreen("高级", listOf("调试" to "", "清空本地数据" to ""), onBack)
        else -> SimpleSettingsScreen("关于", listOf("版本" to "1.0", "许可证" to "GPLv3"), onBack)
    }
}
