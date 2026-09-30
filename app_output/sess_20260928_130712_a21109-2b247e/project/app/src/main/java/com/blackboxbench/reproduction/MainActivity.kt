package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

sealed class Screen {
    object Decks : Screen()
    data class Add(val noteId: Long?, val deck: String) : Screen()
    data class Review(val deck: String) : Screen()
    object Browser : Screen()
    object Statistics : Screen()
    object Settings : Screen()
    object GeneralSettings : Screen()
    object NoteTypes : Screen()
    data class CardTemplate(val noteType: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent { BenchmarkAppTheme { AppRoot() } }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = remember { AppState(context) }
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Decks)) }
    var globalSnackbar by remember { mutableStateOf<String?>(null) }
    val current = stack.last()

    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (stack.size > 1) {
            pop()
        } else {
            (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                onSelect = { target ->
                    scope.launch { drawerState.close() }
                    when (target) {
                        "牌组" -> stack = listOf(Screen.Decks)
                        "卡片浏览器" -> stack = listOf(Screen.Decks, Screen.Browser)
                        "统计" -> stack = listOf(Screen.Decks, Screen.Statistics)
                        else -> stack = listOf(Screen.Decks, Screen.Settings)
                    }
                }
            )
        }
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF9F9FF))) {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (current) {
                is Screen.Decks -> DeckListScreen(
                    app = app,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onStudy = { deck -> push(Screen.Review(deck)) },
                    onAdd = { noteId, deck -> push(Screen.Add(noteId, deck)) },
                    onBrowse = { stack = listOf(Screen.Decks, Screen.Browser) },
                    onNoteTypes = { push(Screen.NoteTypes) },
                    onSettings = { push(Screen.Settings) },
                    incomingSnackbar = globalSnackbar,
                    onSnackbarShown = { globalSnackbar = null }
                )
                is Screen.Add -> AddNoteScreen(
                    app = app,
                    noteId = current.noteId,
                    initialDeck = current.deck,
                    onBack = { pop() }
                )
                is Screen.Review -> ReviewerScreen(
                    app = app,
                    deck = current.deck,
                    onBack = { pop() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onEditNote = { noteId -> push(Screen.Add(noteId, current.deck)) },
                    onComplete = {
                        globalSnackbar = "恭喜！你已经完成了今天的学习任务。"
                        pop()
                    }
                )
                is Screen.Browser -> BrowserScreen(
                    app = app,
                    onBack = { pop() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenNote = { noteId -> push(Screen.Add(noteId, "")) }
                )
                is Screen.Statistics -> StatisticsScreen(
                    app = app,
                    onBack = { pop() },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
                is Screen.Settings -> SettingsScreen(
                    app = app,
                    onBack = { pop() },
                    onGeneral = { push(Screen.GeneralSettings) },
                    onNoteTypes = { push(Screen.NoteTypes) }
                )
                is Screen.GeneralSettings -> GeneralSettingsScreen(app = app, onBack = { pop() })
                is Screen.NoteTypes -> NoteTypesScreen(
                    app = app,
                    onBack = { pop() },
                    onOpen = { name -> push(Screen.CardTemplate(name)) }
                )
                is Screen.CardTemplate -> CardTemplateScreen(
                    app = app,
                    noteType = current.noteType,
                    onBack = { pop() }
                )
            }
            }
        }
    }
}

@Composable
private fun AppDrawer(onSelect: (String) -> Unit) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Surface(color = AnkiColors.DrawerHeader, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 20.dp, top = 26.dp, bottom = 26.dp)) {
                Text("AnkiDroid", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(8.dp))
        DrawerRow("牌组", "\u25A4", selected = true) { onSelect("牌组") }
        DrawerRow("卡片浏览器", "\u26C1", selected = false) { onSelect("卡片浏览器") }
        DrawerRow("统计", "\u2197", selected = false) { onSelect("统计") }
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = AnkiColors.RowDivider)
        DrawerRow("设置", "\u2699", selected = false) { onSelect("设置") }
        DrawerRow("帮助", "?", selected = false) { onSelect("帮助") }
        DrawerRow("支持 AnkiDroid", "\u2661", selected = false) { onSelect("支持 AnkiDroid") }
    }
}

@Composable
private fun DrawerRow(label: String, glyph: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(if (selected) AnkiColors.PrimaryLight else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Text(
                glyph,
                color = if (selected) AnkiColors.PrimaryDark else AnkiColors.TextSecondary,
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (selected) AnkiColors.PrimaryDark else AnkiColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
