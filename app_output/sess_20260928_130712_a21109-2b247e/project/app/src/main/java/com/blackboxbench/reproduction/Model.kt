package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class CState { NEW, LEARNING, REVIEW }

data class Deck(
    val name: String,
    val filtered: Boolean = false,
    val filter: String = "",
    val limit: Int = 100,
    val order: String = "乱序"
)

data class NoteType(
    val name: String,
    val fields: List<String>,
    val templates: List<String>
)

data class Note(
    val id: Long,
    val type: String,
    val deck: String,
    val fields: List<String>,
    val tags: List<String> = emptyList()
) {
    val sortField: String get() = fields.firstOrNull().orEmpty()
}

data class Card(
    val id: Long,
    val noteId: Long,
    val deck: String,
    val template: String,
    val state: CState = CState.NEW,
    val step: Int = 0,
    val intervalDays: Double = 0.0,
    val dueDay: Long = today(),
    val marked: Boolean = false,
    val flag: Int = 0,
    val suspended: Boolean = false,
    val buried: Boolean = false
)

/** Day index (local) used as the scheduling unit. */
fun today(): Long {
    val c = Calendar.getInstance()
    return c.get(Calendar.YEAR).toLong() * 1000 + c.get(Calendar.DAY_OF_YEAR)
}

fun dayLabel(day: Long): String {
    val year = (day / 1000).toInt()
    val doy = (day % 1000).toInt()
    val c = Calendar.getInstance()
    c.set(Calendar.YEAR, year)
    c.set(Calendar.DAY_OF_YEAR, doy)
    return "%04d-%02d-%02d".format(
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
    )
}

class AppState(private val context: Context) {

    val decks = mutableStateListOf<Deck>()
    val noteTypes = mutableStateListOf<NoteType>()
    val notes = mutableStateListOf<Note>()
    val cards = mutableStateListOf<Card>()

    var studiedCards by mutableStateOf(0)
    var studySeconds by mutableStateOf(0.0)
    var reviewedToday by mutableStateOf(0)
    var correctToday by mutableStateOf(0)

    private var nextId = 1L
    private val prefs = context.getSharedPreferences("ankidroid_repro", Context.MODE_PRIVATE)

    init {
        if (!restore()) seed()
    }

    fun newId(): Long = nextId++

    // ---------------------------------------------------------------- counts

    private fun inDeck(card: Card, deck: String): Boolean =
        card.deck == deck || card.deck.startsWith("$deck::")

    private fun countsFor(deck: String): IntArray {
        val sel = cards.filter { inDeck(it, deck) && !it.suspended && !it.buried }
        val n = sel.count { it.state == CState.NEW }
        val l = sel.count { it.state == CState.LEARNING }
        val d = sel.count { it.state == CState.REVIEW && it.dueDay <= today() }
        return intArrayOf(n, l, d)
    }

    fun deckCounts(deck: String): Triple<Int, Int, Int> {
        val c = countsFor(deck)
        return Triple(c[0], c[1], c[2])
    }

    fun totalDue(): Int = cards.count {
        !it.suspended && !it.buried &&
            (it.state == CState.NEW || it.state == CState.LEARNING ||
                (it.state == CState.REVIEW && it.dueDay <= today()))
    }

    fun studyQueue(deck: String): MutableList<Long> {
        val sel = cards.filter { inDeck(it, deck) && !it.suspended && !it.buried }
        val list = ArrayList<Long>()
        sel.filter { it.state == CState.LEARNING }.forEach { list.add(it.id) }
        sel.filter { it.state == CState.NEW }.take(20).forEach { list.add(it.id) }
        sel.filter { it.state == CState.REVIEW && it.dueDay <= today() }.forEach { list.add(it.id) }
        return list
    }

    fun cardById(id: Long): Card? = cards.firstOrNull { it.id == id }
    fun noteById(id: Long): Note? = notes.firstOrNull { it.id == id }
    fun noteTypeByName(name: String): NoteType? = noteTypes.firstOrNull { it.name == name }

    // ---------------------------------------------------------------- edits

    fun addDeck(name: String) {
        if (name.isBlank()) return
        if (decks.none { it.name == name }) decks.add(Deck(name))
        persist()
    }

    fun addFilteredDeck(name: String, filter: String, limit: Int, order: String) {
        decks.add(0, Deck(name, filtered = true, filter = filter, limit = limit, order = order))
        persist()
    }

    fun renameDeck(old: String, new: String) {
        val idx = decks.indexOfFirst { it.name == old }
        if (idx >= 0) decks[idx] = decks[idx].copy(name = new)
        for (i in cards.indices) {
            val d = cards[i].deck
            if (d == old || d.startsWith("$old::")) {
                cards[i] = cards[i].copy(deck = new + d.removePrefix(old))
            }
        }
        for (i in notes.indices) {
            val d = notes[i].deck
            if (d == old || d.startsWith("$old::")) {
                notes[i] = notes[i].copy(deck = new + d.removePrefix(old))
            }
        }
        persist()
    }

    fun deleteDeck(name: String) {
        val doomed = cards.filter { inDeck(it, name) }.map { it.id }
        cards.removeAll { it.id in doomed }
        val noteIds = notes.filter { inDeck2(it.deck, name) }.map { it.id }
        notes.removeAll { it.id in noteIds }
        decks.removeAll { it.name == name || it.name.startsWith("$name::") }
        persist()
    }

    private fun inDeck2(deck: String, prefix: String) =
        deck == prefix || deck.startsWith("$prefix::")

    fun emptyDeck(name: String) {
        val ids = cards.filter { inDeck(it, name) }.map { it.noteId }.toSet()
        cards.removeAll { it.noteId in ids }
        notes.removeAll { it.id in ids }
        persist()
    }

    fun rebuildFilteredDeck(deck: Deck) {
        // Re-select cards matching the stored query (deck:<name> is:<status>).
        val m = Regex("deck:([^ ]+)").find(deck.filter)
        val target = m?.groupValues?.get(1) ?: deck.name
        val due = deck.filter.contains("is:due")
        val selected = cards.filter {
            inDeck(it, target) && !it.suspended && !it.buried &&
                (!due || it.state != CState.NEW)
        }.take(deck.limit)
        for (c in selected) {
            val i = cards.indexOfFirst { it.id == c.id }
            if (i >= 0) cards[i] = cards[i].copy(deck = deck.name)
        }
        persist()
    }

    fun addNote(type: String, deck: String, fields: List<String>, tags: List<String>): Long {
        val noteId = newId()
        notes.add(Note(noteId, type, deck, fields, tags))
        val nt = noteTypeByName(type)
        val templates = nt?.templates ?: listOf("卡片 1")
        for (t in templates) {
            cards.add(Card(id = newId(), noteId = noteId, deck = deck, template = t))
        }
        persist()
        return noteId
    }

    fun updateNote(noteId: Long, fields: List<String>, tags: List<String>, deck: String) {
        val i = notes.indexOfFirst { it.id == noteId }
        if (i >= 0) {
            notes[i] = notes[i].copy(fields = fields, tags = tags, deck = deck)
            for (j in cards.indices) {
                if (cards[j].noteId == noteId) cards[j] = cards[j].copy(deck = deck)
            }
        }
        persist()
    }

    fun deleteNote(noteId: Long) {
        notes.removeAll { it.id == noteId }
        cards.removeAll { it.noteId == noteId }
        persist()
    }

    fun tagNote(noteId: Long, tag: String) {
        val i = notes.indexOfFirst { it.id == noteId }
        if (i >= 0 && tag.isNotBlank() && tag !in notes[i].tags) {
            notes[i] = notes[i].copy(tags = notes[i].tags + tag)
        }
        persist()
    }

    fun allTags(): List<String> = notes.flatMap { it.tags }.distinct().sorted()

    fun setCardMarked(cardId: Long, marked: Boolean) {
        val i = cards.indexOfFirst { it.id == cardId }
        if (i >= 0) cards[i] = cards[i].copy(marked = marked)
        persist()
    }

    fun setCardFlag(cardId: Long, flag: Int) {
        val i = cards.indexOfFirst { it.id == cardId }
        if (i >= 0) cards[i] = cards[i].copy(flag = flag)
        persist()
    }

    fun setCardSuspended(cardId: Long, suspended: Boolean) {
        val i = cards.indexOfFirst { it.id == cardId }
        if (i >= 0) cards[i] = cards[i].copy(suspended = suspended)
        persist()
    }

    fun setCardBuried(cardId: Long, buried: Boolean) {
        val i = cards.indexOfFirst { it.id == cardId }
        if (i >= 0) cards[i] = cards[i].copy(buried = buried)
        persist()
    }

    /**
     * Simplified SM-2 scheduler. Returns the interval label shown on the answer
     * button before rating.
     */
    fun intervalLabel(card: Card, rating: Int): String {
        return when {
            card.state == CState.NEW -> when (rating) {
                0 -> "<1 分"; 1 -> "<6 分"; 2 -> "<10 分"; else -> "4 天"
            }
            card.state == CState.LEARNING -> when (rating) {
                0 -> "<1 分"; 1 -> "<10 分"; 2 -> "1 天"; else -> "4 天"
            }
            else -> {
                val base = if (card.intervalDays <= 0) 1.0 else card.intervalDays
                when (rating) {
                    0 -> "<10 分"
                    1 -> "${(base * 1.2).toInt().coerceAtLeast(1)} 天"
                    2 -> "${(base * 2.5).toInt().coerceAtLeast(1)} 天"
                    else -> "${(base * 4).toInt().coerceAtLeast(1)} 天"
                }
            }
        }
    }

    /** Applies a rating and returns true when the card stays in the session. */
    fun answer(cardId: Long, rating: Int): Boolean {
        val i = cards.indexOfFirst { it.id == cardId }
        if (i < 0) return false
        val c = cards[i]
        reviewedToday += 1
        if (rating > 0) correctToday += 1
        var stays = false
        val updated = when (rating) {
            0 -> { // 重来
                stays = true
                c.copy(state = CState.LEARNING, step = 0, intervalDays = 0.0, dueDay = today())
            }
            1 -> { // 困难
                if (c.state == CState.REVIEW) {
                    val d = (c.intervalDays * 1.2).coerceAtLeast(1.0)
                    c.copy(intervalDays = d, dueDay = today() + d.toLong())
                } else {
                    stays = true
                    c.copy(state = CState.LEARNING, step = 1, intervalDays = 0.0, dueDay = today())
                }
            }
            2 -> { // 良好
                if (c.state == CState.REVIEW) {
                    val d = (c.intervalDays * 2.5).coerceAtLeast(1.0)
                    c.copy(intervalDays = d, dueDay = today() + d.toLong())
                } else if (c.state == CState.NEW) {
                    stays = true
                    c.copy(state = CState.LEARNING, step = 1, intervalDays = 0.0, dueDay = today())
                } else {
                    c.copy(state = CState.REVIEW, step = 0, intervalDays = 1.0, dueDay = today() + 1)
                }
            }
            else -> { // 简单
                val d = if (c.intervalDays <= 0) 4.0 else c.intervalDays * 4
                c.copy(state = CState.REVIEW, step = 0, intervalDays = d, dueDay = today() + d.toLong())
            }
        }
        cards[i] = updated
        persist()
        return stays
    }

    fun bumpStudy(seconds: Double) {
        studySeconds += seconds
        persist()
    }

    fun bumpStudiedCards(n: Int) {
        studiedCards += n
        persist()
    }

    fun resetStatsFooter() {
        // no-op hook kept for symmetry
    }

    // ------------------------------------------------------------- seed/load

    private fun seed() {
        noteTypes.clear()
        noteTypes.addAll(
            listOf(
                NoteType("图片遮盖", listOf("图片"), listOf("卡片 1")),
                NoteType("填空题", listOf("文本", "附加"), listOf("卡片 1")),
                NoteType("问答题", listOf("正面", "背面"), listOf("卡片 1")),
                NoteType("问答题（可选附翻转卡片）", listOf("正面", "背面", "添加翻转卡片"), listOf("卡片 1", "卡片 2")),
                NoteType("问答题（输入答案）", listOf("正面", "背面"), listOf("卡片 1")),
                NoteType("问答题（附翻转卡片）", listOf("正面", "背面"), listOf("卡片 1", "卡片 2"))
            )
        )

        decks.clear()
        decks.addAll(
            listOf(
                "默认牌组", "语言", "语言::日语", "语言::日语::五十音", "语言::日语::词汇",
                "语言::英语", "语言::英语::核心词汇", "科学", "科学::天文", "科学::化学",
                "历史", "历史::近代史"
            ).map { Deck(it) }
        )

        // (deck, front, back, state)
        val seedCards = listOf(
            // 语言::日语::五十音  new=3
            Triple("语言::日语::五十音", "あ", "a") to CState.NEW,
            Triple("语言::日语::五十音", "い", "i") to CState.NEW,
            Triple("语言::日语::五十音", "う", "u") to CState.NEW,
            // 语言::日语::词汇  new=3 due=1
            Triple("语言::日语::词汇", "おはようございます", "早上好") to CState.NEW,
            Triple("语言::日语::词汇", "ありがとう", "谢谢") to CState.NEW,
            Triple("语言::日语::词汇", "すみません", "对不起/劳驾") to CState.NEW,
            Triple("语言::日语::词汇", "こんにちは", "你好") to CState.REVIEW,
            // 语言::英语::核心词汇  new=2 learn=1
            Triple("语言::英语::核心词汇", "apple", "苹果") to CState.NEW,
            Triple("语言::英语::核心词汇", "book", "书") to CState.NEW,
            Triple("语言::英语::核心词汇", "water", "水") to CState.LEARNING,
            // 科学::天文  new=3 due=2
            Triple("科学::天文", "太阳系最大的行星是？", "木星") to CState.NEW,
            Triple("科学::天文", "光从太阳到地球约需多久？", "约 8 分 20 秒") to CState.NEW,
            Triple("科学::天文", "月球自转与公转周期有何关系？", "几乎相等（潮汐锁定）") to CState.NEW,
            Triple("科学::天文", "距离太阳最近的行星是？", "水星") to CState.REVIEW,
            Triple("科学::天文", "银河系中心位于哪个星座方向？", "人马座") to CState.REVIEW,
            // 科学::化学  new=2
            Triple("科学::化学", "元素符号 Au 表示？", "金") to CState.NEW,
            Triple("科学::化学", "常温下唯一的液态金属？", "汞（Hg）") to CState.NEW,
            // 历史::近代史  new=1
            Triple("历史::近代史", "第一次月球着陆是哪一年？", "1969 年") to CState.NEW
        )

        var tagIndex = 0
        for ((t, st) in seedCards) {
            val noteId = newId()
            val tags = when {
                t.first.startsWith("语言::日语") -> listOf("日语")
                t.first.startsWith("语言::英语") -> listOf("英语")
                t.first.startsWith("科学::天文") -> listOf("天文")
                t.first.startsWith("科学::化学") -> listOf("化学")
                else -> listOf("历史")
            }
            tagIndex++
            notes.add(Note(noteId, "问答题", t.first, listOf(t.second, t.third), tags))
            cards.add(
                Card(
                    id = newId(),
                    noteId = noteId,
                    deck = t.first,
                    template = "卡片 1",
                    state = st,
                    intervalDays = if (st == CState.REVIEW) 3.0 else 0.0,
                    dueDay = if (st == CState.REVIEW) today() else today()
                )
            )
        }
        studiedCards = 0
        studySeconds = 0.0
        reviewedToday = 0
        correctToday = 0
        persist()
    }

    fun resetToSeed() {
        seed()
    }

    // ------------------------------------------------------------- persistence

    fun persist() {
        try {
            val root = JSONObject()
            root.put("nextId", nextId)
            root.put("studiedCards", studiedCards)
            root.put("studySeconds", studySeconds)
            root.put("reviewedToday", reviewedToday)
            root.put("correctToday", correctToday)
            val ds = JSONArray()
            for (d in decks) ds.put(
                JSONObject().put("name", d.name).put("filtered", d.filtered)
                    .put("filter", d.filter).put("limit", d.limit).put("order", d.order)
            )
            root.put("decks", ds)
            val nts = JSONArray()
            for (n in noteTypes) nts.put(
                JSONObject().put("name", n.name).put("fields", JSONArray(n.fields))
                    .put("templates", JSONArray(n.templates))
            )
            root.put("noteTypes", nts)
            val ns = JSONArray()
            for (n in notes) ns.put(
                JSONObject().put("id", n.id).put("type", n.type).put("deck", n.deck)
                    .put("fields", JSONArray(n.fields)).put("tags", JSONArray(n.tags))
            )
            root.put("notes", ns)
            val cs = JSONArray()
            for (c in cards) cs.put(
                JSONObject().put("id", c.id).put("noteId", c.noteId).put("deck", c.deck)
                    .put("template", c.template).put("state", c.state.name).put("step", c.step)
                    .put("interval", c.intervalDays).put("dueDay", c.dueDay)
                    .put("marked", c.marked).put("flag", c.flag)
                    .put("suspended", c.suspended).put("buried", c.buried)
            )
            root.put("cards", cs)
            prefs.edit().putString("state", root.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun restore(): Boolean {
        val raw = prefs.getString("state", null) ?: return false
        return try {
            val root = JSONObject(raw)
            nextId = root.optLong("nextId", 1L)
            studiedCards = root.optInt("studiedCards", 0)
            studySeconds = root.optDouble("studySeconds", 0.0)
            reviewedToday = root.optInt("reviewedToday", 0)
            correctToday = root.optInt("correctToday", 0)
            decks.clear(); noteTypes.clear(); notes.clear(); cards.clear()
            val ds = root.getJSONArray("decks")
            for (i in 0 until ds.length()) {
                val o = ds.getJSONObject(i)
                decks.add(
                    Deck(
                        o.getString("name"), o.optBoolean("filtered"),
                        o.optString("filter"), o.optInt("limit", 100), o.optString("order", "乱序")
                    )
                )
            }
            val nts = root.getJSONArray("noteTypes")
            for (i in 0 until nts.length()) {
                val o = nts.getJSONObject(i)
                noteTypes.add(
                    NoteType(o.getString("name"), o.getJSONArray("fields").toStringList(), o.getJSONArray("templates").toStringList())
                )
            }
            val ns = root.getJSONArray("notes")
            for (i in 0 until ns.length()) {
                val o = ns.getJSONObject(i)
                notes.add(
                    Note(
                        o.getLong("id"), o.getString("type"), o.getString("deck"),
                        o.getJSONArray("fields").toStringList(), o.getJSONArray("tags").toStringList()
                    )
                )
            }
            val cs = root.getJSONArray("cards")
            for (i in 0 until cs.length()) {
                val o = cs.getJSONObject(i)
                cards.add(
                    Card(
                        o.getLong("id"), o.getLong("noteId"), o.getString("deck"), o.getString("template"),
                        CState.valueOf(o.getString("state")), o.optInt("step"), o.optDouble("interval"),
                        o.optLong("dueDay", today()), o.optBoolean("marked"), o.optInt("flag"),
                        o.optBoolean("suspended"), o.optBoolean("buried")
                    )
                )
            }
            decks.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}

private fun JSONArray.toStringList(): List<String> {
    val out = ArrayList<String>(length())
    for (i in 0 until length()) out.add(getString(i))
    return out
}
