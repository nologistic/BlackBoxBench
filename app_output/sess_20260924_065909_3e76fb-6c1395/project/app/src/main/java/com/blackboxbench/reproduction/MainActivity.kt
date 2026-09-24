package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    Store.revision
    val s = Store.settings
    val dark = when (s.theme) {
        "暗色", "黑色" -> true
        "亮色" -> false
        else -> isSystemInDarkTheme()
    }
    val p = if (dark) DarkPalette else LightPalette
    BenchmarkAppTheme(dark) {
        Surface(modifier = Modifier.fillMaxSize(), color = p.background) {
          Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            val start = remember {
                if (Store.justSeeded) Nav.Welcome
                else Nav.Main(if (s.openLastList) s.lastViewKey else "my")
            }
            val router = remember { Router(start) }

            BackHandler(enabled = router.canPop && router.current !is Nav.Main) { router.pop() }

            when (val nav = router.current) {
                is Nav.Welcome -> WelcomeScreen(
                    p,
                    onContinue = {
                        Store.justSeeded = false
                        s.lastViewKey = "my"
                        Store.save()
                        router.reset(Nav.Main("my"))
                    },
                    onAccounts = { router.push(Nav.Accounts) }
                )
                is Nav.Accounts -> AccountsScreen(p) { router.pop() }
                is Nav.Main -> MainScreen(p, router, nav.viewKey) { key ->
                    s.lastViewKey = key
                    Store.save()
                    router.reset(Nav.Main(key))
                }
                is Nav.Edit -> TaskEditScreen(p, router, nav.taskId, nav.listId) { taskId ->
                    router.push(Nav.TagPick(taskId))
                }
                is Nav.TagPick -> TagPickerScreen(p, router, nav.taskId)
                is Nav.ListEdit -> ListEditScreen(p, router, nav.listId)
                is Nav.TagEdit -> TagEditScreen(p, router, nav.tagId)
                is Nav.FilterPresets -> FilterPresetsScreen(p, router)
                is Nav.FilterEdit -> FilterEditScreen(p, router, nav.preset)
                is Nav.Search -> SearchScreen(p, router, nav.from)
                is Nav.Settings -> SettingsScreen(p, router, nav.page)
            }
          }
        }
    }
}
