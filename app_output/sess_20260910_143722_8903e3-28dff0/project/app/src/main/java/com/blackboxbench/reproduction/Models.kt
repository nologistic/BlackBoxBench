package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val notebook: String,
    val title: String,
    val body: String,
    val tags: List<String> = emptyList(),
    val isTodo: Boolean = false,
    val completed: Boolean = false,
    val deleted: Boolean = false,
    val pinned: Boolean = false,
    val updated: Long = System.currentTimeMillis()
)

val welcomeBody = """# Welcome to Joplin!

Joplin is a free, open source note taking and to-do application.

## Markdown

Write **bold text**, *italic text*, and `inline code`.

- Create notebooks
- Organise notes with tags
- [x] Read the welcome note
- [ ] Create your first note

> Your notes stay available offline.
"""

val projectBody = """# 复现工程周记

> 每周五更新，记录本周进展与下周计划。

## 本周进展

| 日期 | 事项 | 状态 |
|---|---|---|
| 周一 | 沙盒素材结构定稿 | 完成 |
| 周三 | 交互原型 | 完成 |
| 周五 | 评测清单 | 进行中 |

## 关键代码

```
fun nextOccurrence(today: LocalDate) =
    today.plusDays(1)
```

## 待办

- [x] 提交素材包清单评审
- [ ] 补充电子书样例
- [ ] 撰写评审 few-shot
"""

fun defaultNotes(): List<NoteItem> = listOf(
    NoteItem(id="welcome-1", notebook="👋 欢迎!", title="1. Welcome to Joplin!", body=welcomeBody, tags=listOf("入门"), pinned=true, updated=6),
    NoteItem(id="welcome-2", notebook="👋 欢迎!", title="2. Importing and exporting notes", body="# Importing and exporting notes\n\nUse JEX files to move a complete notebook between devices.", updated=5),
    NoteItem(id="welcome-3", notebook="👋 欢迎!", title="3. Synchronising your notes", body="# Synchronising your notes\n\nChoose Joplin Cloud, Dropbox, OneDrive, WebDAV or a local file system.", updated=4),
    NoteItem(id="welcome-4", notebook="👋 欢迎!", title="4. Tips", body="# Tips\n\n- Search titles and bodies\n- Use tags\n- Attach files and drawings", updated=3),
    NoteItem(id="welcome-5", notebook="👋 欢迎!", title="5. Joplin Privacy Policy", body="# Joplin Privacy Policy\n\nYour notes are yours.", updated=2),
    NoteItem(id="project-weekly", notebook="复现工程", title="复现工程周记", body=projectBody, tags=listOf("工作","周记"), updated=10),
    NoteItem(id="project-todo", notebook="复现工程", title="设计评审待办", body="- [x] 确定配色\n- [ ] 图标切图\n- [ ] 标注间距", tags=listOf("工作"), isTodo=true, updated=9),
    NoteItem(id="recipe", notebook="食谱", title="番茄鸡蛋面", body="## 食材\n- 番茄 2 个\n- 鸡蛋 2 个\n\n## 步骤\n1. 番茄去皮切块\n2. 炒蛋盛出\n3. 番茄炒出汁后合炒", tags=listOf("食谱"), updated=8)
)

object NoteStore {
    private const val PREF = "joplin_clone_data"
    private const val KEY_NOTES = "notes"
    private const val KEY_BOOKS = "notebooks"
    private const val KEY_SELECTED = "selected"

    fun loadNotes(context: Context): List<NoteItem> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_NOTES, null)
            ?: return defaultNotes()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val tagArray = o.optJSONArray("tags") ?: JSONArray()
                NoteItem(
                    id=o.getString("id"),
                    notebook=o.getString("notebook"),
                    title=o.optString("title"),
                    body=o.optString("body"),
                    tags=(0 until tagArray.length()).map { tagArray.getString(it) },
                    isTodo=o.optBoolean("todo"),
                    completed=o.optBoolean("completed"),
                    deleted=o.optBoolean("deleted"),
                    pinned=o.optBoolean("pinned"),
                    updated=o.optLong("updated")
                )
            }
        }.getOrElse { defaultNotes() }
    }

    fun saveNotes(context: Context, notes: List<NoteItem>) {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(JSONObject().apply {
                put("id", n.id); put("notebook", n.notebook); put("title", n.title)
                put("body", n.body); put("tags", JSONArray(n.tags)); put("todo", n.isTodo)
                put("completed", n.completed); put("deleted", n.deleted); put("pinned", n.pinned)
                put("updated", n.updated)
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_NOTES, array.toString()).apply()
    }

    fun loadNotebooks(context: Context): List<String> {
        val raw=context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_BOOKS,null)
        return raw?.let { runCatching { val a=JSONArray(it); (0 until a.length()).map(a::getString) }.getOrNull() }
            ?: listOf("👋 欢迎!","Project","入门","工作","  复现工程","  会议纪要","生活","  食谱")
    }

    fun saveNotebooks(context: Context, books: List<String>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_BOOKS, JSONArray(books).toString()).apply()
    }

    fun loadSelected(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_SELECTED, "👋 欢迎!") ?: "👋 欢迎!"

    fun saveSelected(context: Context, selected: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_SELECTED, selected).apply()
    }


}
