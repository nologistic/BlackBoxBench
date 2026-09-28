package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** A single article, normalised from RSS / Atom / JSON Feed. */
data class Article(
    val id: String,
    val feedId: String,
    val title: String,
    val link: String,
    val published: Long,
    val contentHtml: String,
    val summary: String,
    val image: String?,
    var read: Boolean = false,
    var saved: Boolean = false,
)

/** A subscription. */
data class Feed(
    val id: String,
    val title: String,
    val url: String,
    val description: String = "",
    var customTitle: String? = null,
    var fetchFullArticle: Boolean = false,
    var enhanceThumbnails: Boolean = false,
    var notifications: Boolean = false,
    var skipDuplicates: Boolean = false,
    var generateUniqueId: Boolean = false,
    var openMode: Int = 0,
    val articles: MutableList<Article> = mutableListOf(),
) {
    val displayTitle: String get() = customTitle?.takeIf { it.isNotBlank() } ?: title
    val unreadCount: Int get() = articles.count { !it.read }
}

/**
 * In-memory application state plus lightweight persistence. The synthetic
 * subscriptions bundled in assets/ are used when a URL is resolved offline.
 */
class FeederState(private val context: Context) {

    val feeds = mutableStateListOf<Feed>()

    var theme by mutableStateOf("系统")
    var dynamicColor by mutableStateOf(true)
    var darkTheme by mutableStateOf("黑色")
    var filterSummary by mutableStateOf(false)
    var filterLinks by mutableStateOf(false)
    var textFont by mutableStateOf("Roboto")
    var textScale by mutableStateOf(1.0f)
    var syncInterval by mutableStateOf("每小时")
    var syncOnStart by mutableStateOf(false)
    var syncWifiOnly by mutableStateOf(false)
    var syncChargingOnly by mutableStateOf(false)
    var maxItems by mutableStateOf(100)
    var articleSort by mutableStateOf("从新到旧")
    var showFab by mutableStateOf(true)
    var showFeedsAfterMarkRead by mutableStateOf(false)
    var articleStyle by mutableStateOf("卡片")
    var maxLines by mutableStateOf(2)
    var titlesOnly by mutableStateOf(false)
    var swipeToRead by mutableStateOf("仅从右侧")
    var autoMarkRead by mutableStateOf(false)
    var showThumbnails by mutableStateOf(true)
    var showReadingTime by mutableStateOf(false)
    var showUnreadCount by mutableStateOf(false)
    var forceSingleColumn by mutableStateOf(false)
    var contentOpenMode by mutableStateOf("阅读器")
    var linkOpenMode by mutableStateOf("自定义标签页")
    var builtInPlayer by mutableStateOf(true)
    var paginationMode by mutableStateOf(false)
    var imagesWifiOnly by mutableStateOf(false)
    var ttsDetectLanguage by mutableStateOf(true)

    init {
        if (!restore()) seed()
    }

    // ------------------------------------------------------------- bundled data

    private val assetByUrl = mapOf(
        "https://news.blackboxbench.example/feed.rss" to "feeds/blackbox_times.rss",
        "https://tech.blackboxbench.example/atom.xml" to "feeds/tech_digest.atom",
        "https://design.blackboxbench.example/feed.json" to "feeds/design_notes.json",
    )

    private val imageCycle = listOf(
        "shelf.png", "lake.png", "mint.png", "cookies.png",
        "baking.png", "morning.png", "notifications.png", "room.png", "walk.png",
    )

    fun knownAssetFor(url: String): String? = assetByUrl[url.trim()]

    fun hasAssetFeedText(name: String): Boolean = name.isNotBlank()

    private fun readAsset(path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }

    // ----------------------------------------------------------------- mutation

    private fun seed() {
        feeds.add(
            Feed(
                id = "feed-feeder-news",
                title = "Feeder News",
                url = "https://news.nononsenseapps.com/index.atom",
                description = "Feeder 官方资讯源",
            )
        )
        persist()
    }

    private fun nextImage(seedIndex: Int): String = imageCycle[seedIndex % imageCycle.size]

    fun addFeedFromAsset(url: String): Feed? {
        val asset = knownAssetFor(url) ?: return null
        val existing = feeds.firstOrNull { it.url == url }
        if (existing != null) return existing
        val parsed = FeedParser.parse(readAsset(asset), url)
        val feedId = "feed-" + url.hashCode().toString().replace("-", "n")
        val feed = Feed(
            id = feedId,
            title = parsed.title,
            url = url,
            description = parsed.description,
        )
        parsed.articles.forEachIndexed { index, item ->
            feed.articles.add(
                Article(
                    id = item.id,
                    feedId = feedId,
                    title = item.title,
                    link = item.link,
                    published = item.published,
                    contentHtml = item.contentHtml,
                    summary = item.summary,
                    image = nextImage(index),
                )
            )
        }
        feed.articles.sortByDescending { it.published }
        feeds.add(feed)
        persist()
        return feed
    }

    fun addEmptyFeed(url: String, title: String): Feed {
        val existing = feeds.firstOrNull { it.url == url }
        if (existing != null) return existing
        val feedId = "feed-" + url.hashCode().toString().replace("-", "n")
        val feed = Feed(id = feedId, title = title.ifBlank { url }, url = url)
        feeds.add(feed)
        persist()
        return feed
    }

    fun importOpmlText(text: String): Int {
        val outlines = FeedParser.parseOpml(text)
        var added = 0
        outlines.forEach { outline ->
            val before = feeds.size
            if (knownAssetFor(outline.xmlUrl) != null) {
                addFeedFromAsset(outline.xmlUrl)
            } else {
                addEmptyFeed(outline.xmlUrl, outline.text)
            }
            if (feeds.size > before) added++
        }
        persist()
        return added
    }

    fun exportOpml(): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<opml version=\"2.0\">\n  <head>\n")
        builder.append("    <title>Feeder 订阅导出</title>\n")
        builder.append("  </head>\n  <body>\n")
        feeds.forEach { feed ->
            val text = feed.displayTitle.replace("\"", "&quot;")
            builder.append("    <outline type=\"rss\" text=\"$text\" xmlUrl=\"${feed.url}\"/>\n")
        }
        builder.append("  </body>\n</opml>\n")
        return builder.toString()
    }

    fun writeExport(name: String, content: String): String {
        val file = java.io.File(context.filesDir, name)
        file.writeText(content)
        return file.absolutePath
    }

    fun listExportFiles(): List<String> =
        (context.filesDir.listFiles() ?: emptyArray())
            .filter { it.name.endsWith(".opml") }
            .map { it.name }
            .sorted()

    fun readExport(name: String): String? =
        java.io.File(context.filesDir, name).takeIf { it.exists() }?.readText()

    fun readBundledOpml(): String? = try {
        readAsset("subscriptions.opml")
    } catch (_: Exception) {
        null
    }

    fun markAllRead() {
        feeds.forEach { feed -> feed.articles.forEach { it.read = true } }
        persist()
    }

    fun markRead(article: Article) {
        if (!article.read) {
            article.read = true
            persist()
        }
    }

    fun toggleSaved(article: Article) {
        article.saved = !article.saved
        persist()
    }

    fun removeFeeds(ids: Set<String>) {
        feeds.removeAll { it.id in ids }
        persist()
    }

    fun allArticles(): List<Article> = feeds.flatMap { it.articles }

    fun savedArticles(): List<Article> = allArticles().filter { it.saved }

    fun recentArticles(): List<Article> = allArticles().filter { it.read }

    fun findArticle(id: String): Article? = allArticles().firstOrNull { it.id == id }

    fun findFeed(id: String): Feed? = feeds.firstOrNull { it.id == id }

    // -------------------------------------------------------------- persistence

    fun persist() {
        val root = JSONObject()
        val feedArray = JSONArray()
        feeds.forEach { feed ->
            val feedObject = JSONObject()
            feedObject.put("id", feed.id)
            feedObject.put("title", feed.title)
            feedObject.put("url", feed.url)
            feedObject.put("description", feed.description)
            feedObject.put("customTitle", feed.customTitle ?: JSONObject.NULL)
            feedObject.put("fetchFullArticle", feed.fetchFullArticle)
            feedObject.put("enhanceThumbnails", feed.enhanceThumbnails)
            feedObject.put("notifications", feed.notifications)
            feedObject.put("skipDuplicates", feed.skipDuplicates)
            feedObject.put("generateUniqueId", feed.generateUniqueId)
            feedObject.put("openMode", feed.openMode)
            val articleArray = JSONArray()
            feed.articles.forEach { article ->
                articleArray.put(
                    JSONObject().apply {
                        put("id", article.id)
                        put("feedId", article.feedId)
                        put("title", article.title)
                        put("link", article.link)
                        put("published", article.published)
                        put("content", article.contentHtml)
                        put("summary", article.summary)
                        put("image", article.image ?: JSONObject.NULL)
                        put("read", article.read)
                        put("saved", article.saved)
                    }
                )
            }
            feedObject.put("articles", articleArray)
            feedArray.put(feedObject)
        }
        root.put("feeds", feedArray)
        root.put("theme", theme)
        root.put("dynamicColor", dynamicColor)
        root.put("darkTheme", darkTheme)
        root.put("filterSummary", filterSummary)
        root.put("filterLinks", filterLinks)
        root.put("textFont", textFont)
        root.put("textScale", textScale.toDouble())
        root.put("syncInterval", syncInterval)
        root.put("syncOnStart", syncOnStart)
        root.put("syncWifiOnly", syncWifiOnly)
        root.put("syncChargingOnly", syncChargingOnly)
        root.put("maxItems", maxItems)
        root.put("articleSort", articleSort)
        root.put("showFab", showFab)
        root.put("showFeedsAfterMarkRead", showFeedsAfterMarkRead)
        root.put("articleStyle", articleStyle)
        root.put("maxLines", maxLines)
        root.put("titlesOnly", titlesOnly)
        root.put("swipeToRead", swipeToRead)
        root.put("autoMarkRead", autoMarkRead)
        root.put("showThumbnails", showThumbnails)
        root.put("showReadingTime", showReadingTime)
        root.put("showUnreadCount", showUnreadCount)
        root.put("forceSingleColumn", forceSingleColumn)
        root.put("contentOpenMode", contentOpenMode)
        root.put("linkOpenMode", linkOpenMode)
        root.put("builtInPlayer", builtInPlayer)
        root.put("paginationMode", paginationMode)
        root.put("imagesWifiOnly", imagesWifiOnly)
        root.put("ttsDetectLanguage", ttsDetectLanguage)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_STATE, root.toString()).apply()
    }

    private fun restore(): Boolean {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_STATE, null)
            ?: return false
        return try {
            val root = JSONObject(raw)
            val feedArray = root.optJSONArray("feeds") ?: JSONArray()
            if (feedArray.length() == 0) return false
            feeds.clear()
            for (i in 0 until feedArray.length()) {
                val feedObject = feedArray.getJSONObject(i)
                val feed = Feed(
                    id = feedObject.getString("id"),
                    title = feedObject.optString("title"),
                    url = feedObject.optString("url"),
                    description = feedObject.optString("description"),
                    customTitle = feedObject.optString("customTitle").takeIf { !feedObject.isNull("customTitle") },
                    fetchFullArticle = feedObject.optBoolean("fetchFullArticle"),
                    enhanceThumbnails = feedObject.optBoolean("enhanceThumbnails"),
                    notifications = feedObject.optBoolean("notifications"),
                    skipDuplicates = feedObject.optBoolean("skipDuplicates"),
                    generateUniqueId = feedObject.optBoolean("generateUniqueId"),
                    openMode = feedObject.optInt("openMode"),
                )
                val articleArray = feedObject.optJSONArray("articles") ?: JSONArray()
                for (j in 0 until articleArray.length()) {
                    val articleObject = articleArray.getJSONObject(j)
                    feed.articles.add(
                        Article(
                            id = articleObject.getString("id"),
                            feedId = articleObject.optString("feedId", feed.id),
                            title = articleObject.optString("title"),
                            link = articleObject.optString("link"),
                            published = articleObject.optLong("published"),
                            contentHtml = articleObject.optString("content"),
                            summary = articleObject.optString("summary"),
                            image = articleObject.optString("image").takeIf { !articleObject.isNull("image") },
                            read = articleObject.optBoolean("read"),
                            saved = articleObject.optBoolean("saved"),
                        )
                    )
                }
                feeds.add(feed)
            }
            theme = root.optString("theme", theme)
            dynamicColor = root.optBoolean("dynamicColor", dynamicColor)
            darkTheme = root.optString("darkTheme", darkTheme)
            filterSummary = root.optBoolean("filterSummary", filterSummary)
            filterLinks = root.optBoolean("filterLinks", filterLinks)
            textFont = root.optString("textFont", textFont)
            textScale = root.optDouble("textScale", textScale.toDouble()).toFloat()
            syncInterval = root.optString("syncInterval", syncInterval)
            syncOnStart = root.optBoolean("syncOnStart", syncOnStart)
            syncWifiOnly = root.optBoolean("syncWifiOnly", syncWifiOnly)
            syncChargingOnly = root.optBoolean("syncChargingOnly", syncChargingOnly)
            maxItems = root.optInt("maxItems", maxItems)
            articleSort = root.optString("articleSort", articleSort)
            showFab = root.optBoolean("showFab", showFab)
            showFeedsAfterMarkRead = root.optBoolean("showFeedsAfterMarkRead", showFeedsAfterMarkRead)
            articleStyle = root.optString("articleStyle", articleStyle)
            maxLines = root.optInt("maxLines", maxLines)
            titlesOnly = root.optBoolean("titlesOnly", titlesOnly)
            swipeToRead = root.optString("swipeToRead", swipeToRead)
            autoMarkRead = root.optBoolean("autoMarkRead", autoMarkRead)
            showThumbnails = root.optBoolean("showThumbnails", showThumbnails)
            showReadingTime = root.optBoolean("showReadingTime", showReadingTime)
            showUnreadCount = root.optBoolean("showUnreadCount", showUnreadCount)
            forceSingleColumn = root.optBoolean("forceSingleColumn", forceSingleColumn)
            contentOpenMode = root.optString("contentOpenMode", contentOpenMode)
            linkOpenMode = root.optString("linkOpenMode", linkOpenMode)
            builtInPlayer = root.optBoolean("builtInPlayer", builtInPlayer)
            paginationMode = root.optBoolean("paginationMode", paginationMode)
            imagesWifiOnly = root.optBoolean("imagesWifiOnly", imagesWifiOnly)
            ttsDetectLanguage = root.optBoolean("ttsDetectLanguage", ttsDetectLanguage)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val PREFS = "feeder_state"
        private const val KEY_STATE = "state"
    }
}
