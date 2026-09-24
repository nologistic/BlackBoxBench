package com.blackboxbench.reproduction

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File

/** Decodes and caches thumbnails / full size bitmaps from disk. */
object Thumbs {
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }

    fun thumbnail(file: File, sizePx: Int, isVideo: Boolean): Bitmap? {
        val key = "t:${file.absolutePath}:$sizePx"
        cache.get(key)?.let { return it }
        val bitmap = if (isVideo) videoFrame(file) else decode(file, sizePx)
        if (bitmap != null) cache.put(key, bitmap)
        return bitmap
    }

    fun full(file: File, isVideo: Boolean): Bitmap? {
        val key = "f:${file.absolutePath}"
        cache.get(key)?.let { return it }
        val bitmap = if (isVideo) videoFrame(file) else decode(file, 1600)
        if (bitmap != null) cache.put(key, bitmap)
        return bitmap
    }

    private fun decode(file: File, target: Int): Bitmap? {
        if (!file.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (_: Throwable) {
            null
        }
    }

    private fun videoFrame(file: File): Bitmap? = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val frame = retriever.getFrameAtTime(0)
        retriever.release()
        frame
    } catch (_: Throwable) {
        null
    }

    /** Cheap glyphs are kept across refreshes; only explicit invalidations clear the cache. */
    fun invalidate() {
        cache.evictAll()
    }
}
