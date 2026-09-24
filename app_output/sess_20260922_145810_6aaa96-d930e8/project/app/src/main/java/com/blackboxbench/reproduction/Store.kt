package com.blackboxbench.reproduction

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One row of the file browser. */
data class Entry(
    val file: File,
    val name: String,
    val isDir: Boolean,
    val modified: Long,
)

/** Notebook on internal storage; the UI reports it as the shared Documents folder. */
class Store(private val context: Context) {

    val root: File = File(context.filesDir, "notebook")

    init {
        if (!root.exists()) root.mkdirs()
    }

    fun list(dir: File): List<Entry> =
        (dir.listFiles() ?: emptyArray())
            .filter { !it.name.startsWith(".") }
            .map { Entry(it, it.name, it.isDirectory, it.lastModified()) }
            .sortedWith(compareByDescending<Entry> { it.isDir }.thenBy { it.name.lowercase(Locale.ROOT) })

    fun isRoot(dir: File): Boolean = dir.absolutePath == root.absolutePath

    fun create(dir: File, name: String, isDir: Boolean): File? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val target = File(dir, clean)
        if (target.exists()) return null
        return if (isDir) {
            if (target.mkdirs()) target else null
        } else {
            target.parentFile?.mkdirs()
            if (target.createNewFile()) target else null
        }
    }

    fun rename(file: File, newName: String): File? {
        val clean = newName.trim()
        if (clean.isEmpty()) return null
        val target = File(file.parentFile, clean)
        if (target.exists()) return null
        return if (file.renameTo(target)) target else null
    }

    fun delete(file: File): Boolean = file.deleteRecursively()

    fun read(file: File): String = if (file.exists()) file.readText() else ""

    fun write(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    fun displayPath(dir: File?): String {
        val base = "$VISIBLE_ROOT/$NOTEBOOK_DIR"
        if (dir == null) return VISIBLE_ROOT
        val rel = dir.absolutePath.removePrefix(root.absolutePath).trimStart('/')
        return if (rel.isEmpty()) base else "$base/$rel"
    }

    fun parentOf(dir: File): File? = if (isRoot(dir)) null else dir.parentFile

    fun child(dir: File, name: String): File = File(dir, name)

    fun quickNote(): File = File(root, "QuickNote.md").also { if (!it.exists()) write(it, "") }

    fun todoFile(): File = File(root, "todo.txt").also { if (!it.exists()) write(it, "") }

    /** All files below the notebook, used by the recursive search. */
    fun walk(dir: File, depth: Int, maxDepth: Int): List<File> {
        val out = ArrayList<File>()
        val children = (dir.listFiles() ?: emptyArray()).sortedBy { it.name.lowercase(Locale.ROOT) }
        for (f in children) {
            if (f.name.startsWith(".")) continue
            out.add(f)
            if (f.isDirectory && (maxDepth < 0 || depth < maxDepth)) out.addAll(walk(f, depth + 1, maxDepth))
        }
        return out
    }

    companion object {
        const val VISIBLE_ROOT = "/storage/emulated/0/Documents"
        const val NOTEBOOK_DIR = "markor"

        private val stampFormat = SimpleDateFormat("yyyy/M/d HH:mm", Locale.ROOT)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        private val auditFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)

        fun stamp(ms: Long): String = stampFormat.format(Date(ms))
        fun today(): String = dateFormat.format(Date())
        fun audit(ms: Long): String = auditFormat.format(Date(ms))
    }
}

/** Small persisted settings bag; reads are observable from composition. */
class Prefs(context: Context) {

    private val sp: SharedPreferences = context.getSharedPreferences("markor_repro", Context.MODE_PRIVATE)
    private val cache = mutableStateMapOf<String, Any>()

    fun str(key: String, def: String): String =
        cache.getOrPut(key) { sp.getString(key, def) ?: def } as String

    fun setStr(key: String, value: String) {
        cache[key] = value
        sp.edit().putString(key, value).apply()
    }

    fun bool(key: String, def: Boolean): Boolean =
        cache.getOrPut(key) { sp.getBoolean(key, def) } as Boolean

    fun setBool(key: String, value: Boolean) {
        cache[key] = value
        sp.edit().putBoolean(key, value).apply()
    }

    fun int(key: String, def: Int): Int =
        cache.getOrPut(key) { sp.getInt(key, def) } as Int

    fun setInt(key: String, value: Int) {
        cache[key] = value
        sp.edit().putInt(key, value).apply()
    }

    fun exportToJson(): String {
        val sb = StringBuilder("{\n")
        val keys = sp.all.keys.sorted()
        keys.forEachIndexed { index, key ->
            val value = sp.all[key]
            sb.append("  \"").append(key).append("\": ")
            when (value) {
                is Boolean -> sb.append(value)
                is Int -> sb.append(value)
                else -> sb.append('"').append(value.toString().replace("\"", "'")).append('"')
            }
            if (index != keys.lastIndex) sb.append(',')
            sb.append('\n')
        }
        sb.append("}\n")
        return sb.toString()
    }

    fun importFromJson(text: String): Int {
        var applied = 0
        val body = text.trim().removePrefix("{").removeSuffix("}")
        body.split(',').forEach { pair ->
            val idx = pair.indexOf(':')
            if (idx <= 0) return@forEach
            val key = pair.substring(0, idx).trim().trim('"')
            val raw = pair.substring(idx + 1).trim()
            if (key.isEmpty()) return@forEach
            when {
                raw == "true" || raw == "false" -> {
                    val v = raw.toBoolean()
                    cache[key] = v
                    sp.edit().putBoolean(key, v).apply()
                    applied++
                }

                raw.startsWith("\"") -> {
                    val v = raw.trim('"')
                    cache[key] = v
                    sp.edit().putString(key, v).apply()
                    applied++
                }

                raw.toIntOrNull() != null -> {
                    val v = raw.toInt()
                    cache[key] = v
                    sp.edit().putInt(key, v).apply()
                    applied++
                }
            }
        }
        return applied
    }
}
