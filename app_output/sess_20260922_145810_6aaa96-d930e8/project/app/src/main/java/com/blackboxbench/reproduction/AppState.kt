package com.blackboxbench.reproduction

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

enum class NoteMode { MARKDOWN, TASK, PLAIN }

/** Shared, screen independent app state. */
class AppState(val context: Context, val store: Store, val prefs: Prefs) {

    var dir by mutableStateOf(store.root)
    var currentFile by mutableStateOf<File?>(null)
    var selection by mutableStateOf<Set<String>>(emptySet())
    var message by mutableStateOf<String?>(null)

    fun modeOf(file: File): NoteMode = when {
        file.name.equals("todo.txt", ignoreCase = true) -> NoteMode.TASK
        file.name.endsWith(".md", ignoreCase = true) -> NoteMode.MARKDOWN
        else -> NoteMode.PLAIN
    }

    fun toast(text: String) {
        message = text
    }

    fun copyToClipboard(label: String, text: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareText(file: File, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        toast("已打开分享面板")
    }

    fun exportHtml(file: File, text: String): File {
        val target = File(file.parentFile, file.nameWithoutExtension + ".html")
        store.write(target, toHtml(file.name, text))
        return target
    }

    fun exportIcs(file: File, text: String): File {
        val target = File(file.parentFile, file.nameWithoutExtension + ".ics")
        val body = buildString {
            append("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Markor//Reproduction//CN\n")
            text.split('\n').filter { it.isNotBlank() }.forEachIndexed { index, line ->
                append("BEGIN:VEVENT\n")
                append("UID:markor-").append(index).append("@local\n")
                append("DTSTART;VALUE=DATE:").append(Store.today().replace("-", "")).append('\n')
                append("SUMMARY:").append(line.trim().replace("\n", " ")).append('\n')
                append("END:VEVENT\n")
            }
            append("END:VCALENDAR\n")
        }
        store.write(target, body)
        return target
    }

    fun exportPng(file: File, text: String): File {
        val target = File(file.parentFile, file.nameWithoutExtension + ".png")
        val bitmap = Bitmap.createBitmap(1080, 1600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(33, 33, 33)
            textSize = 34f
        }
        val header = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(120, 120, 120)
            textSize = 26f
        }
        canvas.drawText(file.name, 48f, 72f, header)
        var y = 140f
        text.split('\n').forEach { line ->
            if (y < 1560f) {
                canvas.drawText(line.take(60), 48f, y, paint)
                y += 48f
            }
        }
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return target
    }

    private fun toHtml(title: String, text: String): String {
        val body = StringBuilder()
        var inList = false
        text.split('\n').forEach { line ->
            val trimmed = line.trim()
            val ordered = Regex("^(\\d+)\\.\\s+(.*)$").find(trimmed)
            val bullet = Regex("^[-*+]\\s+(.*)$").find(trimmed)
            val heading = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
            when {
                heading != null -> {
                    if (inList) { body.append("</ul>\n"); inList = false }
                    val level = heading.groupValues[1].length
                    body.append("<h").append(level).append('>')
                        .append(escape(heading.groupValues[2])).append("</h").append(level).append(">\n")
                }

                ordered != null -> {
                    body.append("<ol><li>").append(escape(ordered.groupValues[2])).append("</li></ol>\n")
                }

                bullet != null -> {
                    body.append("<ul><li>").append(escape(bullet.groupValues[1])).append("</li></ul>\n")
                }

                trimmed.isEmpty() -> {
                    if (inList) { body.append("</ul>\n"); inList = false }
                    body.append("<p></p>\n")
                }

                else -> body.append("<p>").append(escape(trimmed)).append("</p>\n")
            }
        }
        return """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"><title>${escape(title)}</title>
            <style>body{font-family:sans-serif;margin:32px;line-height:1.6;color:#212121}
            h1,h2{border-bottom:1px solid #ddd;padding-bottom:4px}</style></head>
            <body>
            $body
            </body></html>
        """.trimIndent()
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

fun fileIntent(context: Context, file: File, mime: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "分享").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
