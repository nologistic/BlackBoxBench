package com.blackboxbench.reproduction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Minimal forward/back stack: an empty stack means the library root. */
class Navigator {
    val stack = mutableStateListOf<Pair<String, String>>()

    val current: Pair<String, String>? get() = stack.lastOrNull()

    fun push(route: String, arg: String = "") {
        stack.add(route to arg)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
    }

    fun reset() {
        stack.clear()
    }
}

/** Central place every screen uses to open the shared dialogs. */
class DialogController {
    var songDetails by mutableStateOf<Song?>(null)
    var tagEditor by mutableStateOf<Song?>(null)
    var addToPlaylist by mutableStateOf<List<String>?>(null)
    var newPlaylist by mutableStateOf<List<String>?>(null)
    var renamePlaylist by mutableStateOf<String?>(null)
    var saveAsPlaylist by mutableStateOf<String?>(null)
    var deletePlaylist by mutableStateOf<String?>(null)
    var themePicker by mutableStateOf(false)
    var categories by mutableStateOf(false)
    var sleepTimer by mutableStateOf(false)
    var ringtone by mutableStateOf(false)
    var changelog by mutableStateOf(false)
    var licenses by mutableStateOf(false)
    var replayGain by mutableStateOf(false)
    var blacklistInfo by mutableStateOf(false)
    var whitelistInfo by mutableStateOf(false)
    var exportInfo by mutableStateOf(false)
    var equalizer by mutableStateOf(false)
}

val LocalNav = staticCompositionLocalOf { Navigator() }
val LocalDialogs = staticCompositionLocalOf { DialogController() }
val LocalApp = staticCompositionLocalOf<AppState> { error("AppState missing") }
