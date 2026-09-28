package com.blackboxbench.reproduction

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Simulated playback engine. It keeps an accurate position clock and queue
 * semantics so every visible behaviour (progress, auto-advance, repeat,
 * shuffle, mini player) matches the observed app. Real audio output is not
 * required for the reproduction.
 */
class PlayerController(private val repo: MusicRepository) {

    var queue by mutableStateOf<List<Long>>(emptyList())
        private set
    var index by mutableIntStateOf(-1)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionSec by mutableFloatStateOf(0f)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(0) // 0 off, 1 all, 2 one
        private set
    var sleepRemainingSec by mutableIntStateOf(0) // 0 = off
        private set

    val current: Song? get() = queue.getOrNull(index)?.let { repo.songById(it) }

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (isPlaying) {
                val dur = current?.durationSec ?: 0
                positionSec += 0.5f
                if (dur > 0 && positionSec >= dur) {
                    onTrackFinished()
                }
                if (sleepRemainingSec > 0) {
                    sleepRemainingSec -= 1
                    if (sleepRemainingSec <= 0) {
                        sleepRemainingSec = 0
                        pause()
                    }
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    init {
        handler.postDelayed(tick, 500)
    }

    fun playQueue(ids: List<Long>, startIndex: Int, startPlaying: Boolean = true) {
        if (ids.isEmpty()) return
        queue = ids
        index = startIndex.coerceIn(0, ids.size - 1)
        positionSec = 0f
        isPlaying = startPlaying
        current?.let { repo.recordPlay(it.id) }
        persist()
    }

    fun playSong(songId: Long, contextIds: List<Long>) {
        val i = contextIds.indexOf(songId)
        playQueue(contextIds, if (i >= 0) i else 0)
    }

    fun togglePlayPause() {
        if (current == null) return
        isPlaying = !isPlaying
        if (isPlaying) current?.let { repo.recordPlay(it.id) }
        persist()
    }

    fun pause() {
        isPlaying = false
        persist()
    }

    fun next() {
        if (queue.isEmpty()) return
        if (shuffle) {
            index = (0 until queue.size).random()
        } else if (index + 1 < queue.size) {
            index++
        } else if (repeatMode == 1) {
            index = 0
        } else {
            positionSec = 0f
            isPlaying = false
            persist()
            return
        }
        positionSec = 0f
        isPlaying = true
        current?.let { repo.recordPlay(it.id) }
        persist()
    }

    fun previous() {
        if (queue.isEmpty()) return
        if (positionSec > 5f) {
            positionSec = 0f
            return
        }
        index = if (index - 1 >= 0) index - 1 else queue.lastIndex
        positionSec = 0f
        isPlaying = true
        current?.let { repo.recordPlay(it.id) }
        persist()
    }

    fun seekTo(fraction: Float) {
        val dur = current?.durationSec ?: 0
        positionSec = (fraction.coerceIn(0f, 1f)) * dur
    }

    fun toggleShuffle() {
        shuffle = !shuffle
        persist()
    }

    fun cycleRepeat() {
        repeatMode = (repeatMode + 1) % 3
        persist()
    }

    fun setSleepTimer(seconds: Int) {
        sleepRemainingSec = seconds
    }

    fun cancelSleepTimer() {
        sleepRemainingSec = 0
    }

    fun clearQueue() {
        queue = emptyList()
        index = -1
        isPlaying = false
        positionSec = 0f
        persist()
    }

    fun playNext(songId: Long) {
        val list = queue.toMutableList()
        if (index < 0) {
            list.add(songId)
        } else {
            list.add(index + 1, songId)
        }
        queue = list
    }

    fun addToQueue(songId: Long) {
        if (queue.contains(songId)) return
        queue = queue + songId
        persist()
    }

    private fun onTrackFinished() {
        if (repeatMode == 2) {
            positionSec = 0f
            return
        }
        next()
    }

    fun persist() {
        repo.prefs.edit()
            .putString("p_queue", queue.joinToString(","))
            .putInt("p_index", index)
            .putFloat("p_pos", positionSec)
            .putBoolean("p_shuffle", shuffle)
            .putInt("p_repeat", repeatMode)
            .apply()
    }

    fun restore() {
        val q = repo.prefs.getString("p_queue", null)?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        if (q.isEmpty()) return
        queue = q
        index = repo.prefs.getInt("p_index", 0).coerceIn(0, q.size - 1)
        positionSec = repo.prefs.getFloat("p_pos", 0f)
        shuffle = repo.prefs.getBoolean("p_shuffle", false)
        repeatMode = repo.prefs.getInt("p_repeat", 0)
        isPlaying = false
    }
}
