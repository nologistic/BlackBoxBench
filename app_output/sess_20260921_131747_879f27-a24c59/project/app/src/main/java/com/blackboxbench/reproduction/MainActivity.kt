package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    BenchmarkAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navigation = rememberNavController()
    var notice by remember { mutableStateOf("") }
    val start = if (Repo.onboarded) "home" else "welcome"

    NavHost(navController = navigation, startDestination = start) {
        composable("welcome") {
            WelcomeScreen(
                notice = notice,
                onDismissNotice = { notice = "" },
                onContinueWithoutSync = {
                    Repo.completeOnboarding()
                    navigation.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onAddAccount = {
                    notice = "当前离线演示环境无法连接账户服务，请使用「继续但不同步」，任务会保存在本机。"
                },
                onImportBackup = {
                    notice = "未在设备上找到 Tasks.org 备份文件。示例数据已内置，可直接使用「继续但不同步」。"
                }
            )
        }
        composable("home") {
            HomeScreen(
                onOpenTask = { id -> navigation.navigate("task/$id") },
                onNewTask = { listId -> navigation.navigate("edit?taskId=&listId=$listId") },
                onOpenSettings = { navigation.navigate("settings") }
            )
        }
        composable(
            route = "task/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("taskId").orEmpty()
            TaskDetailScreen(
                taskId = id,
                onBack = { navigation.popBackStack() },
                onEdit = { navigation.navigate("edit?taskId=$it&listId=") },
                onDeleted = { navigation.popBackStack() }
            )
        }
        composable(
            route = "edit?taskId={taskId}&listId={listId}",
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType; defaultValue = "" },
                navArgument("listId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId").orEmpty().ifBlank { null }
            val listId = entry.arguments?.getString("listId").orEmpty()
            TaskEditScreen(
                taskId = taskId,
                defaultListId = listId,
                onDone = { navigation.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navigation.popBackStack() })
        }
    }
}
