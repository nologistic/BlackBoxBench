package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val NavyBar = Color(0xFF21253A)
val NavyBarDark = Color(0xFF1B1E30)
val MarkorRed = Color(0xFFE4483D)
val EditorBg = Color(0xFFF5F5F5)
val ListBg = Color(0xFFE8E8E8)
val FavYellow = Color(0xFFF2B01E)

data class NoteFile(
    val name: String,
    val modified: Long,
    var favorite: Boolean = false,
) {
    val isTodo: Boolean get() = name == "todo.txt"
    val isQuickNote: Boolean get() = name == "QuickNote.md"
}

object Store {
    private lateinit var appContext: Context
    private lateinit var notebookDir: File
    private val favorites = mutableSetOf<String>()
    var onboarded: Boolean = false
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        notebookDir = File(appContext.filesDir, "notebook").apply { mkdirs() }
        val prefs = appContext.getSharedPreferences("markor", Context.MODE_PRIVATE)
        onboarded = prefs.getBoolean("onboarded", false)
        favorites.clear()
        favorites.addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet())
    }

    fun markOnboarded() {
        onboarded = true
        appContext.getSharedPreferences("markor", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarded", true).apply()
    }

    private fun saveFavorites() {
        appContext.getSharedPreferences("markor", Context.MODE_PRIVATE)
            .edit().putStringSet("favorites", favorites.toSet()).apply()
    }

    fun listFiles(): List<NoteFile> =
        (notebookDir.listFiles()?.toList() ?: emptyList())
            .sortedBy { it.name.lowercase() }
            .map { NoteFile(it.name, it.lastModified(), favorites.contains(it.name)) }

    fun file(name: String): File = File(notebookDir, name)

    fun read(name: String): String = file(name).let { if (it.exists()) it.readText() else "" }

    fun write(name: String, content: String) {
        file(name).writeText(content)
    }

    fun exists(name: String): Boolean = file(name).exists()

    fun delete(name: String) {
        file(name).delete()
        if (favorites.remove(name)) saveFavorites()
    }

    fun rename(old: String, new: String): Boolean {
        val ok = file(old).renameTo(file(new))
        if (ok && favorites.remove(old)) {
            favorites.add(new)
            saveFavorites()
        }
        return ok
    }

    fun setFavorite(name: String, fav: Boolean) {
        if (fav) favorites.add(name) else favorites.remove(name)
        saveFavorites()
    }

    fun isFavorite(name: String): Boolean = favorites.contains(name)

    fun sizeOf(name: String): Long = file(name).let { if (it.exists()) it.length() else 0L }

    fun sha256(name: String): String {
        val f = file(name)
        if (!f.exists()) return "-"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(f.readBytes())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ----- templates for the new-file dialog -----
    val templates = listOf(
        "空文件", "asciidoc-reference.adoc", "cooking-recipe.md",
        "hugo-post-front-matter.md", "jekyll-post.adoc", ".md",
        "markor-markdown-reference.md", "orgmode-reference.org",
        "presentation-beamer.md", "sample.csv", "todo.example.txt", "zettelkasten.md",
    )

    val fileTypes = listOf("Markdown", "纯文本", "todo.txt", "Wikitext", "Zim", "AsciiDoc", "OrgMode", "CSV")

    fun templateContent(template: String, title: String): String = when (template) {
        "空文件" -> ""
        "cooking-recipe.md" -> "# $title\n\n## Ingredients\n\n- \n\n## Steps\n\n1. \n"
        "todo.example.txt" -> "(A) Example task @context +project\nx 2026-09-22 Done task\n"
        "sample.csv" -> "name,value\nexample,1\n"
        "zettelkasten.md" -> "# $title\n\nTags: \n\n## Notes\n\n"
        "markor-markdown-reference.md" -> "# Markdown Reference\n\n**bold** *italic* `code`\n\n- list item\n\n> quote\n"
        else -> "# $title\n"
    }
}

/** Markor auto-format punctuation replacement, applied while editing. */
fun autoFormat(input: String): String {
    var out = input
    out = out.replace("##", "# #")
    out = out.replace(Regex("(?m)(^|\\s)1\\.(?= )")) { m -> m.groupValues[1] + "1。" }
    out = out.replace(Regex("(?m)(^|\\s)-(?= )")) { m -> m.groupValues[1] + "—" }
    out = out.replace("*", "∗")
    out = out.replace(">", "》")
    out = out.replace(": ", "： ")
    return out
}

/**
 * IME-friendly variant: when the change is a pure append (normal typing),
 * transform only the newly inserted suffix so the composing region before
 * the cursor is never rewritten mid-composition.
 */
fun autoFormatAppend(old: String, newValue: String): String =
    if (newValue.length > old.length && newValue.startsWith(old)) {
        old + autoFormat(newValue.substring(old.length))
    } else {
        autoFormat(newValue)
    }

fun formatTime(millis: Long): String =
    SimpleDateFormat("yyyy/M/d HH:mm", Locale.US).format(Date(millis))

fun formatTimeFull(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))

// ---------- todo.txt syntax highlighting ----------
private val PriorityColor = Color(0xFF3F51B5)
private val ContextColor = Color(0xFF00897B)
private val ProjectColor = Color(0xFFE65100)
private val DoneColor = Color(0xFF9E9E9E)
private val DateColor = Color(0xFF7B1FA2)

fun isDoneLine(line: String): Boolean =
    line.startsWith("x ") || line.matches(Regex("^\\d{4}.\\d{2}.\\d{2} x .*"))

fun highlightTodoLine(line: String): AnnotatedString = buildAnnotatedString {
    if (isDoneLine(line)) {
        withStyle(SpanStyle(color = DoneColor)) { append(line) }
        return@buildAnnotatedString
    }
    val tokens = line.split(" ")
    tokens.forEachIndexed { i, token ->
        val style = when {
            i == 0 && token.matches(Regex("^\\([A-Z]\\)$")) -> SpanStyle(color = PriorityColor, fontWeight = FontWeight.Bold)
            token.startsWith("@") -> SpanStyle(color = ContextColor, fontWeight = FontWeight.Bold)
            token.startsWith("+") -> SpanStyle(color = ProjectColor, fontWeight = FontWeight.Bold)
            token.matches(Regex("^\\d{4}.\\d{2}.\\d{2}$")) -> SpanStyle(color = DateColor)
            else -> SpanStyle(color = Color(0xFF222222))
        }
        withStyle(style) { append(token) }
        if (i < tokens.lastIndex) append(" ")
    }
}

fun todoContexts(text: String): List<String> =
    Regex("@\\w+").findAll(text).map { it.value }.distinct().sorted().toList()

fun todoProjects(text: String): List<String> =
    Regex("\\+\\w+").findAll(text).map { it.value }.distinct().sorted().toList()

fun todoPriorities(text: String): List<String> =
    Regex("^\\(([A-Z])\\)", RegexOption.MULTILINE).findAll(text).map { it.value }.distinct().sorted().toList()

fun recentFilesJson(files: List<NoteFile>): String = JSONArray(files.map { it.name }).toString()
