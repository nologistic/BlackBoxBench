package com.blackboxbench.reproduction

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color

enum class LibraryTab(val label: String) {
    SONGS("歌曲"), ALBUMS("专辑"), ARTISTS("艺术家"), GENRES("音乐类型"), PLAYLISTS("播放列表")
}

sealed class Screen {
    data object Library : Screen()
    data class AlbumDetail(val album: String) : Screen()
    data class ArtistDetail(val name: String) : Screen()
    data class GenreDetail(val name: String) : Screen()
    data class PlaylistDetail(val id: Long) : Screen()
    data class SmartDetail(val kind: SmartKind) : Screen()
    data object Folders : Screen()
    data object Settings : Screen()
    data object About : Screen()
    data object Search : Screen()
    data object TagEditor : Screen()
    data object Equalizer : Screen()
}

val PrimaryChoices = listOf(
    Color(0xFF3F51B5), Color(0xFF009688), Color(0xFFF44336), Color(0xFFE91E63),
    Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFFF9800), Color(0xFF2196F3), Color(0xFF607D8B)
)

val AccentChoices = listOf(
    Color(0xFFFF4081), Color(0xFF7C4DFF), Color(0xFF448AFF), Color(0xFF64FFDA),
    Color(0xFFFFAB40), Color(0xFFFF5252), Color(0xFF69F0AE), Color(0xFFFFD740)
)

class Settings(val prefs: SharedPreferences) {

    private val state = mutableStateMapOf<String, Any>()

    private val boolDefaults = mapOf(
        "remember_last_page" to true,
        "whitelist" to false,
        "colored_nav_bar" to true,
        "colored_shortcuts" to true,
        "transparent_widget_bg" to true,
        "classic_notification" to false,
        "colored_notification" to true,
        "show_lyrics" to true,
        "animate_now_playing_icon" to false,
        "show_track_number" to true,
        "lower_volume_focus" to true,
        "gapless" to false,
        "remember_shuffle" to true,
        "favorite_songs" to true,
        "skipped_songs" to false,
        "collect_crash" to false,
        "sync_queue_tags" to false
    )

    private val intDefaults = mapOf(
        "global_theme" to 0,
        "theme_style" to 0,
        "primary_color" to 0,
        "accent_color" to 0,
        "now_playing_appearance" to 0,
        "default_action" to 0,
        "auto_download" to 1,
        "rg_source" to 0,
        "rg_preamp" to 0,
        "recently_added_interval" to 0,
        "recent_interval" to 0,
        "recently_not_played_interval" to 0
    )

    val enabledCategories: SnapshotStateList<Boolean> = mutableStateListOf()

    init {
        boolDefaults.forEach { (k, d) -> state[k] = prefs.getBoolean(k, d) }
        intDefaults.forEach { (k, d) -> state[k] = prefs.getInt(k, d) }
        val csv = prefs.getString("enabled_categories", "1,1,1,1,1") ?: "1,1,1,1,1"
        csv.split(",").forEach { enabledCategories.add(it.trim() != "0") }
        while (enabledCategories.size < LibraryTab.entries.size) enabledCategories.add(true)
    }

    fun bool(key: String): Boolean = state[key] as? Boolean ?: (boolDefaults[key] ?: false)

    fun setBool(key: String, value: Boolean) {
        state[key] = value
        prefs.edit().putBoolean(key, value).apply()
    }

    fun int(key: String): Int = state[key] as? Int ?: (intDefaults[key] ?: 0)

    fun setInt(key: String, value: Int) {
        state[key] = value
        prefs.edit().putInt(key, value).apply()
    }

    fun setCategories(values: List<Boolean>) {
        enabledCategories.clear()
        enabledCategories.addAll(values)
        prefs.edit().putString("enabled_categories", values.joinToString(",") { if (it) "1" else "0" }).apply()
    }

    val primary: Color get() = PrimaryChoices[int("primary_color").coerceIn(0, PrimaryChoices.size - 1)]
    val accent: Color get() = AccentChoices[int("accent_color").coerceIn(0, AccentChoices.size - 1)]
}

class AppModel(context: Context) {
    val repo = MusicRepository(context)
    val settings = Settings(repo.prefs)
    val player = PlayerController(repo)

    var onboardingDone by mutableStateOf(repo.prefs.getBoolean("onboarding_done", false))
        private set

    var tab by mutableStateOf(LibraryTab.entries.getOrElse(repo.prefs.getInt("last_tab", 0)) { LibraryTab.SONGS })
    var backStack: SnapshotStateList<Screen> = mutableStateListOf(Screen.Library)
    var drawerOpen by mutableStateOf(false)
    var nowPlayingExpanded by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var lastFolderPath by mutableStateOf("/storage/emulated/0/Music")

    // transient UI requests
    var pendingSongMenu by mutableStateOf<Long?>(null)
    var songMenuFromPlaylist by mutableStateOf<Long?>(null)
    var tagEditorSong by mutableStateOf<Song?>(null)

    val current: Screen get() = backStack.last()

    fun finishOnboarding() {
        onboardingDone = true
        repo.prefs.edit().putBoolean("onboarding_done", true).apply()
    }

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        // Never restore a transient editor
        val top = backStack.removeAt(backStack.lastIndex)
        if (top is Screen.TagEditor || top is Screen.Search) {
            if (backStack.lastOrNull() is Screen.TagEditor || backStack.lastOrNull() is Screen.Search) pop()
        }
        return true
    }

    fun selectTab(t: LibraryTab) {
        tab = t
        repo.prefs.edit().putInt("last_tab", t.ordinal).apply()
        if (backStack.size > 1) {
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        }
    }

    fun visibleTabs(): List<LibraryTab> = LibraryTab.entries.filter { settings.enabledCategories.getOrElse(it.ordinal) { true } }
}
