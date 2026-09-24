package com.blackboxbench.reproduction

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** 播放引擎：队列 + shuffle/repeat 三态 + 进度推进；音频用素材 WAV 循环。 */
class PlayerEngine(private val context: Context, private val library: List<Song>) {

    val queue = mutableListOf<Song>()
    val queueIndex = mutableIntStateOf(-1)
    val playing = mutableStateOf(false)
    val positionSec = mutableIntStateOf(0)
    val shuffle = mutableStateOf(false)
    /** 0 = 关闭, 1 = 全部循环, 2 = 单曲循环 */
    val repeatMode = mutableIntStateOf(0)

    private var player: MediaPlayer? = null
    private val ticker = android.os.Handler(android.os.Looper.getMainLooper())
    private var elapsedAtTick = 0L

    private val tick = object : Runnable {
        override fun run() {
            val song = current() ?: return
            val next = positionSec.intValue + 1
            if (next >= song.duration) {
                onTrackFinished()
            } else {
                positionSec.intValue = next
            }
            ticker.postDelayed(this, 1000)
        }
    }

    fun current(): Song? = queue.getOrNull(queueIndex.intValue)

    /** 从任意列表播放：重建「当前播放」队列。 */
    fun playQueue(songs: List<Song>, startIndex: Int) {
        queue.clear()
        queue.addAll(if (shuffle.value) songs.shuffled() else songs)
        queueIndex.intValue = if (shuffle.value) 0 else startIndex.coerceIn(0, queue.size - 1)
        positionSec.intValue = 0
        startPlayback()
    }

    private fun startPlayback() {
        val song = current() ?: return
        positionSec.intValue = 0
        startAudio(song)
        playing.value = true
        ticker.removeCallbacks(tick)
        ticker.postDelayed(tick, 1000)
    }

    private fun startAudio(song: Song) {
        try {
            player?.release()
            player = MediaPlayer()
            context.assets.openFd(MusicLibrary.audioFor(song.id)).use { afd ->
                player?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            player?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player?.isLooping = true
            player?.prepare()
            player?.start()
        } catch (_: Exception) {
            player = null
        }
    }

    fun togglePlayPause() {
        if (playing.value) pause() else resume()
    }

    fun pause() {
        playing.value = false
        player?.pause()
        ticker.removeCallbacks(tick)
    }

    fun resume() {
        val song = current() ?: return
        if (positionSec.intValue >= song.duration) positionSec.intValue = 0
        playing.value = true
        try {
            if (player == null) startAudio(song) else player?.start()
        } catch (_: Exception) {
            startAudio(song)
        }
        ticker.removeCallbacks(tick)
        ticker.postDelayed(tick, 1000)
    }

    fun seekTo(seconds: Int) {
        positionSec.intValue = seconds.coerceIn(0, (current()?.duration ?: 0))
    }

    fun next(auto: Boolean = false) {
        if (queue.isEmpty()) return
        val i = queueIndex.intValue
        val last = queue.size - 1
        when {
            auto && repeatMode.intValue == 2 -> startPlayback()
            i >= last -> when {
                auto && repeatMode.intValue == 0 -> { pause(); positionSec.intValue = 0 }
                else -> { queueIndex.intValue = 0; startPlayback() }
            }
            else -> { queueIndex.intValue = i + 1; startPlayback() }
        }
    }

    fun previous() {
        if (queue.isEmpty()) return
        queueIndex.intValue = if (queueIndex.intValue <= 0) queue.size - 1 else queueIndex.intValue - 1
        startPlayback()
    }

    private fun onTrackFinished() {
        next(auto = true)
    }

    fun toggleShuffle() { shuffle.value = !shuffle.value }

    fun cycleRepeat() { repeatMode.intValue = (repeatMode.intValue + 1) % 3 }

    fun release() {
        ticker.removeCallbacks(tick)
        player?.release()
        player = null
    }
}
