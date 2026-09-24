package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** All persisted preferences of the gallery, mirroring the observed settings tree. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("gallery_settings", Context.MODE_PRIVATE)

    /** Bumped whenever a preference changes so composables re-read the values. */
    val revision: MutableState<Int> = mutableIntStateOf(0)

    private fun bump() { revision.value = revision.value + 1 }

    private fun bool(key: String, def: Boolean) = BoolPref(key, def)

    private fun str(key: String, def: String) = StrPref(key, def)

    private fun int(key: String, def: Int) = IntPref(key, def)

    private val reloaders = mutableListOf<() -> Unit>()

    /** Every preference is backed by a snapshot state so the UI refreshes on change. */
    inner class BoolPref(private val key: String, private val def: Boolean) {
        private val state = mutableStateOf(prefs.getBoolean(key, def))
        init { reloaders.add { state.value = prefs.getBoolean(key, def) } }
        fun get(): Boolean = state.value
        fun set(value: Boolean) {
            state.value = value
            prefs.edit().putBoolean(key, value).apply()
            bump()
        }
    }

    inner class StrPref(private val key: String, private val def: String) {
        private val state = mutableStateOf(prefs.getString(key, def) ?: def)
        init { reloaders.add { state.value = prefs.getString(key, def) ?: def } }
        fun get(): String = state.value
        fun set(value: String) {
            state.value = value
            prefs.edit().putString(key, value).apply()
            bump()
        }
    }

    inner class IntPref(private val key: String, private val def: Int) {
        private val state = mutableIntStateOf(prefs.getInt(key, def))
        init { reloaders.add { state.value = prefs.getInt(key, def) } }
        fun get(): Int = state.value
        fun set(value: Int) {
            state.value = value
            prefs.edit().putInt(key, value).apply()
            bump()
        }
    }

    // ------------------------------------------------------------------ 外观
    var theme = str("theme", "系统默认")
    var iconColor = str("iconColor", "绿色")
    var fontSize = str("font", "系统默认")

    // ------------------------------------------------------------------ 常规
    var language = str("language", "中文")
    var dateFormat = str("dateFormat", "自动")
    var fileLoadingPriority = str("fileLoadingPriority", "速度")
    var showHidden = bool("showHidden", false)
    var searchAllFiles = bool("searchAllFiles", false)
    var includeFolders = str("includeFolders", "")
    var excludeFolders = str("excludeFolders", "")

    // ------------------------------------------------------------------ 视频
    var autoplayVideos = bool("autoplayVideos", false)
    var rememberVideoPosition = bool("rememberVideoPosition", false)
    var openVideoWithSystemPlayer = bool("openVideoWithSystemPlayer", false)
    var videoHorizontalGesture = bool("videoHorizontalGesture", false)
    var videoVerticalGesture = bool("videoVerticalGesture", false)

    // ---------------------------------------------------------------- 缩略图
    var cropSquare = bool("cropSquare", false)
    var animateGif = bool("animateGif", false)
    var fileThumbnailStyle = str("fileThumbnailStyle", "方形")
    var folderThumbnailStyle = str("folderThumbnailStyle", "方形")

    // ------------------------------------------------------------------ 滚动
    var horizontalScroll = bool("horizontalScroll", false)
    var pullToRefresh = bool("pullToRefresh", false)

    // -------------------------------------------------------------- 全屏显示
    var hdr = bool("hdr", false)
    var blackBackground = bool("blackBackground", false)
    var hideSystemUi = bool("hideSystemUi", false)
    var edgeTapSwitch = bool("edgeTapSwitch", false)
    var keepScreenOn = bool("keepScreenOn", false)
    var brightnessGesture = bool("brightnessGesture", false)
    var swipeDownExit = bool("swipeDownExit", false)
    var showNotch = bool("showNotch", false)
    var rotationFollowsSystem = bool("rotationFollowsSystem", true)

    // -------------------------------------------------------- 大幅度缩放图像
    var allowDeepZoom = bool("allowDeepZoom", false)
    var allowRotateGesture = bool("allowRotateGesture", false)
    var doubleTapZoom1to1 = bool("doubleTapZoom", false)

    // ------------------------------------------------------------ 更多详细信息
    var showMoreDetails = bool("showMoreDetails", false)

    // ------------------------------------------------------------------ 安全性
    var protectApp = bool("protectApp", false)
    var protectHidden = bool("protectHidden", false)
    var protectDeleteMove = bool("protectDeleteMove", false)
    var passwordType = str("passwordType", "pin")
    var passwordValue = str("passwordValue", "")

    // ---------------------------------------------------------------- 文件操作
    var deleteEmptyFolder = bool("deleteEmptyFolder", false)
    var keepModifiedDate = bool("keepModifiedDate", false)
    var noDeleteConfirm = bool("noDeleteConfirm", false)

    // ---------------------------------------------------------------- 底部按钮
    var showBottomButtons = bool("showBottomButtons", true)
    var bottomButtons = str("bottomButtons", "favorite,edit,share,delete")

    // ------------------------------------------------------------------ 回收站
    var trashEnabled = bool("trashEnabled", true)
    var showRecycleBinInFolders = bool("showRecycleBinInFolders", true)
    var showRecycleBinAtEnd = bool("showRecycleBinAtEnd", false)
    var trashDays = int("trashDays", 30)

    // -------------------------------------------------------------------- 界面
    var sortBy = str("sortBy", "名称")
    var sortAscending = bool("sortAscending", true)
    var columns = int("columns", 2)
    var viewType = str("viewType", "网格")
    var groupBy = str("groupBy", "无")
    var groupByFolder = bool("groupByFolder", false)
    var filterImages = bool("filterImages", true)
    var filterVideos = bool("filterVideos", true)
    var filterGif = bool("filterGif", true)
    var filterRaw = bool("filterRaw", true)
    var filterSvg = bool("filterSvg", true)
    var filterPortrait = bool("filterPortrait", false)
    var defaultFolder = str("defaultFolder", "")
    var showFilenames = bool("showFilenames", true)

    fun setPassword(type: String, value: String) {
        prefs.edit().putString("passwordType", type).putString("passwordValue", value).apply()
        bump()
    }

    fun matches(type: String, value: String): Boolean = passwordType.get() == type && passwordValue.get() == value

    fun anyPasswordSet(): Boolean = passwordValue.get().isNotEmpty()

    fun clearPassword() {
        prefs.edit().putString("passwordValue", "").putBoolean("protectApp", false)
            .putBoolean("protectHidden", false).putBoolean("protectDeleteMove", false).apply()
        reloaders.forEach { it() }
        bump()
    }

    fun saveTo(file: java.io.File) {
        file.writeText(prefs.all.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    fun loadFrom(text: String) {
        val edit = prefs.edit()
        text.lines().filter { it.contains('=') }.forEach { line ->
            val key = line.substringBefore('=')
            val value = line.substringAfter('=')
            when {
                value == "true" || value == "false" -> edit.putBoolean(key, value.toBoolean())
                value.toIntOrNull() != null -> edit.putInt(key, value.toInt())
                else -> edit.putString(key, value)
            }
        }
        edit.apply()
        reloaders.forEach { it() }
        bump()
    }
}

/** Simple in-memory clipboard stand-in for the "copy to clipboard" action. */
object ClipboardHolder {
    var copiedName: MutableState<String?> = mutableStateOf(null)
}
