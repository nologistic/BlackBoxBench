package com.blackboxbench.reproduction

import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Locale
import java.util.TimeZone

/**
 * Parser that unifies RSS 2.0, Atom 1.0 and JSON Feed 1.1 into one model.
 * All content is synthetic test data bundled with the reproduction.
 */
object FeedParser {

    data class ParsedArticle(
        val id: String,
        val title: String,
        val link: String,
        val published: Long,
        val contentHtml: String,
        val summary: String,
    )

    data class ParsedFeed(
        val title: String,
        val description: String,
        val articles: List<ParsedArticle>,
    )

    fun parse(text: String, fallbackTitle: String): ParsedFeed {
        val t = text.trimStart()
        return when {
            t.startsWith("{") -> parseJsonFeed(text, fallbackTitle)
            t.contains("<feed", ignoreCase = true) &&
                t.contains("http://www.w3.org/2005/Atom", ignoreCase = true) ->
                parseAtom(text, fallbackTitle)
            else -> parseRss(text, fallbackTitle)
        }
    }

    // ---------------------------------------------------------------- RSS 2.0

    private fun parseRss(xml: String, fallbackTitle: String): ParsedFeed {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        var event = parser.eventType
        var feedTitle = fallbackTitle
        var feedDescription = ""
        val articles = mutableListOf<ParsedArticle>()
        var inItem = false
        var title = ""
        var link = ""
        var guid = ""
        var pubDate = ""
        var description = ""
        var encoded = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.US)) {
                    "item" -> {
                        inItem = true
                        title = ""; link = ""; guid = ""; pubDate = ""; description = ""; encoded = ""
                    }
                    "title" -> {
                        val value = parser.nextText()
                        if (inItem) title = value else feedTitle = value.ifBlank { fallbackTitle }
                    }
                    "description" -> {
                        val value = parser.nextText()
                        if (inItem) description = value else feedDescription = value
                    }
                    "link" -> {
                        val value = parser.nextText()
                        if (inItem) link = value
                    }
                    "guid" -> {
                        val value = parser.nextText()
                        if (inItem) guid = value
                    }
                    "pubdate" -> {
                        val value = parser.nextText()
                        if (inItem) pubDate = value
                    }
                    "encoded", "content:encoded" -> {
                        val value = parser.nextText()
                        if (inItem) encoded = value
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name.equals("item", ignoreCase = true)) {
                    inItem = false
                    val id = guid.ifBlank { link }.ifBlank { title }
                    articles.add(
                        ParsedArticle(
                            id = id,
                            title = title,
                            link = link,
                            published = parseRfc822(pubDate),
                            contentHtml = encoded.ifBlank { description },
                            summary = description,
                        )
                    )
                }
            }
            event = parser.next()
        }
        return ParsedFeed(feedTitle, feedDescription, articles)
    }

    // ---------------------------------------------------------------- Atom 1.0

    private fun parseAtom(xml: String, fallbackTitle: String): ParsedFeed {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        var event = parser.eventType
        var feedTitle = fallbackTitle
        var feedDescription = ""
        val articles = mutableListOf<ParsedArticle>()
        var inEntry = false
        var title = ""
        var id = ""
        var updated = ""
        var link = ""
        var summary = ""
        var content = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.US)) {
                    "entry" -> {
                        inEntry = true
                        title = ""; id = ""; updated = ""; link = ""; summary = ""; content = ""
                    }
                    "title" -> {
                        val value = parser.nextText()
                        if (inEntry) title = value else feedTitle = value.ifBlank { fallbackTitle }
                    }
                    "subtitle" -> {
                        val value = parser.nextText()
                        if (!inEntry) feedDescription = value
                    }
                    "id" -> {
                        val value = parser.nextText()
                        if (inEntry) id = value
                    }
                    "updated", "published" -> {
                        val value = parser.nextText()
                        if (inEntry && updated.isBlank()) updated = value
                    }
                    "link" -> {
                        val href = parser.getAttributeValue(null, "href")
                        if (inEntry && !href.isNullOrBlank() && link.isBlank()) link = href
                    }
                    "summary" -> {
                        val value = parser.nextText()
                        if (inEntry) summary = value
                    }
                    "content" -> {
                        val value = parser.nextText()
                        if (inEntry) content = value
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name.equals("entry", ignoreCase = true)) {
                    inEntry = false
                    val articleId = id.ifBlank { link }.ifBlank { title }
                    articles.add(
                        ParsedArticle(
                            id = articleId,
                            title = title,
                            link = link,
                            published = parseRfc3339(updated),
                            contentHtml = content.ifBlank { summary },
                            summary = summary,
                        )
                    )
                }
            }
            event = parser.next()
        }
        return ParsedFeed(feedTitle, feedDescription, articles)
    }

    // ---------------------------------------------------------- JSON Feed 1.1

    private fun parseJsonFeed(text: String, fallbackTitle: String): ParsedFeed {
        val root = JSONObject(text)
        val title = root.optString("title").ifBlank { fallbackTitle }
        val description = root.optString("description")
        val items: JSONArray = root.optJSONArray("items") ?: JSONArray()
        val articles = (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            ParsedArticle(
                id = item.optString("id").ifBlank { item.optString("url") }.ifBlank { item.optString("title") },
                title = item.optString("title"),
                link = item.optString("url"),
                published = parseRfc3339(item.optString("date_published")),
                contentHtml = item.optString("content_html").ifBlank { item.optString("summary") },
                summary = item.optString("summary").ifBlank { item.optString("content_text") },
            )
        }
        return ParsedFeed(title, description, articles)
    }

    // ------------------------------------------------------------------- OPML

    data class OpmlOutline(val text: String, val xmlUrl: String, val type: String)

    fun parseOpml(text: String): List<OpmlOutline> {
        val outlines = mutableListOf<OpmlOutline>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(text))
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("outline", ignoreCase = true)) {
                val url = parser.getAttributeValue(null, "xmlUrl")
                    ?: parser.getAttributeValue(null, "xmlurl")
                if (!url.isNullOrBlank()) {
                    val text = parser.getAttributeValue(null, "text")
                        ?: parser.getAttributeValue(null, "title")
                        ?: url
                    val type = parser.getAttributeValue(null, "type") ?: "rss"
                    outlines.add(OpmlOutline(text, url, type))
                }
            }
            event = parser.next()
        }
        return outlines
    }

    // ------------------------------------------------------------------ dates

    private val rfc822Patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    fun parseRfc822(value: String): Long {
        if (value.isBlank()) return System.currentTimeMillis()
        for (pattern in rfc822Patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.timeZone = TimeZone.getTimeZone("GMT")
                return format.parse(value)?.time ?: continue
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return parseRfc3339(value)
    }

    fun parseRfc3339(value: String): Long {
        if (value.isBlank()) return System.currentTimeMillis()
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    .apply { timeZone = TimeZone.getTimeZone("GMT") }
                    .parse(value)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    // ------------------------------------------------------------------- HTML

    fun htmlToParagraphs(html: String): List<String> {
        var value = html
        value = value.replace(Regex("(?i)<br\\s*/?>"), "\n")
        value = value.replace(Regex("(?i)</(p|div|h[1-6]|li|tr|blockquote)>"), "\n")
        value = value.replace(Regex("(?s)<[^>]*>"), "")
        value = decodeEntities(value)
        return value.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun htmlToText(html: String): String {
        val withoutTags = html.replace(Regex("(?s)<[^>]*>"), " ")
        return decodeEntities(withoutTags).replace(Regex("\\s+"), " ").trim()
    }

    fun decodeEntities(value: String): String {
        var result = value
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
        result = Regex("&#x([0-9a-fA-F]+);").replace(result) { match ->
            match.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: match.value
        }
        result = Regex("&#(\\d+);").replace(result) { match ->
            match.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: match.value
        }
        return result
    }
}
