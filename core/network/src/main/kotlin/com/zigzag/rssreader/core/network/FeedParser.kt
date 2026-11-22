package com.zigzag.rssreader.core.network

import com.zigzag.rssreader.core.network.model.ParsedArticle
import com.zigzag.rssreader.core.network.model.ParsedFeed
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.datetime.Instant
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Small dependency-free RSS 2.0 / Atom parser built on the JDK DOM APIs.
 */
class FeedParser @Inject constructor() {
    fun parse(xml: String): ParsedFeed {
        val factory = DocumentBuilderFactory.newInstance()
        configureSafely { factory.isNamespaceAware = true }
        configureSafely { factory.isXIncludeAware = false }
        configureSafely { factory.isExpandEntityReferences = false }
        configureSafely {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        configureSafely {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        }
        configureSafely {
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        configureSafely {
            factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
        }
        configureSafely {
            factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
        }

        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        document.documentElement.normalize()
        val root = document.documentElement
        val isAtom = root.localName?.equals("feed", ignoreCase = true) == true ||
            root.namespaceURI?.contains("w3.org/2005/Atom", ignoreCase = true) == true
        return if (isAtom) parseAtom(root) else parseRss(root)
    }

    private fun parseRss(root: Element): ParsedFeed {
        val channel = children(root, "channel").item(0) as? Element ?: root
        val title = firstText(channel, "title") ?: ""
        val siteUrl = firstText(channel, "link")
        val description = firstText(channel, "description")
        val items = children(channel, "item")
        val articles = (0 until items.length).mapNotNull { index ->
            val item = items.item(index) as? Element ?: return@mapNotNull null
            val itemTitle = firstText(item, "title") ?: ""
            val link = firstText(item, "link") ?: ""
            val guid = firstText(item, "guid")
            val summary = firstText(item, "description")
            val content = firstText(item, "encoded")
            ParsedArticle(
                id = guid?.takeIf { it.isNotBlank() } ?: link,
                title = itemTitle,
                link = link,
                author = firstText(item, "author") ?: firstText(item, "creator"),
                summary = summary,
                contentHtml = content ?: summary,
                publishedAt = parseDate(firstText(item, "pubDate") ?: firstText(item, "date")),
            )
        }
        return ParsedFeed(title, siteUrl, description, articles)
    }

    private fun parseAtom(root: Element): ParsedFeed {
        val feedTitle = firstText(root, "title") ?: ""
        val feedAuthor = rootAuthor(root)
        val entries = children(root, "entry")
        val articles = (0 until entries.length).mapNotNull { index ->
            val entry = entries.item(index) as? Element ?: return@mapNotNull null
            val link = atomLink(entry)
            val id = firstText(entry, "id")
            val summary = firstText(entry, "summary")
            val content = firstText(entry, "content")
            ParsedArticle(
                id = id?.takeIf { it.isNotBlank() } ?: link,
                title = firstText(entry, "title") ?: "",
                link = link,
                author = entryAuthor(entry) ?: feedAuthor,
                summary = summary,
                contentHtml = content ?: summary,
                publishedAt = parseDate(firstText(entry, "published"))
                    ?: parseDate(firstText(entry, "updated")),
            )
        }
        return ParsedFeed(
            title = feedTitle,
            siteUrl = alternateLink(root),
            description = firstText(root, "subtitle"),
            articles = articles,
        )
    }

    private fun children(parent: Element, name: String) =
        parent.getElementsByTagNameNS("*", name)

    private fun firstText(parent: Element, name: String): String? {
        val node = children(parent, name).item(0) ?: return null
        val text = node.textContent?.trim().orEmpty()
        return text.takeIf { it.isNotEmpty() }?.let(::decodeEntities)
    }

    private fun alternateLink(parent: Element): String? {
        val links = children(parent, "link")
        var fallback: String? = null
        for (index in 0 until links.length) {
            val link = links.item(index) as? Element ?: continue
            val href = link.getAttribute("href").trim()
            if (href.isEmpty()) continue
            val rel = link.getAttribute("rel").trim()
            val decoded = decodeEntities(href)
            if (rel.isEmpty() || rel.equals("alternate", ignoreCase = true)) return decoded
            if (fallback == null) fallback = decoded
        }
        return fallback
    }

    private fun atomLink(entry: Element): String = alternateLink(entry).orEmpty()

    private fun rootAuthor(root: Element): String? {
        val author = children(root, "author").item(0) as? Element ?: return null
        return firstText(author, "name")
    }

    private fun entryAuthor(entry: Element): String? {
        val author = children(entry, "author").item(0) as? Element ?: return null
        return firstText(author, "name")
    }

    private fun parseDate(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()
        try {
            return Instant.parse(value)
        } catch (_: Exception) {
            // Fall through to the legacy formats below.
        }
        for (pattern in DATE_PATTERNS) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = try {
                format.parse(value)
            } catch (_: Exception) {
                null
            }
            if (parsed != null) return Instant.fromEpochMilliseconds(parsed.time)
        }
        return null
    }

    private fun configureSafely(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            // Some parsers do not support every hardening option; ignore and keep going.
        }
    }

    private fun decodeEntities(input: String): String = ENTITY_PATTERN.replace(input) { match ->
        val token = match.groupValues[1]
        when {
            token.startsWith("#x", ignoreCase = true) ->
                token.substring(2).toIntOrNull(16)?.let(::codePointToString) ?: match.value
            token.startsWith("#") ->
                token.substring(1).toIntOrNull(10)?.let(::codePointToString) ?: match.value
            else -> NAMED_ENTITIES[token] ?: match.value
        }
    }

    private fun codePointToString(codePoint: Int): String =
        if (codePoint in Character.MIN_CODE_POINT..Character.MAX_CODE_POINT) {
            String(Character.toChars(codePoint))
        } else {
            ""
        }

    private companion object {
        val DATE_PATTERNS = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
        )
        val ENTITY_PATTERN = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")
        val NAMED_ENTITIES = mapOf(
            "amp" to "&",
            "lt" to "<",
            "gt" to ">",
            "quot" to "\"",
            "apos" to "'",
            "nbsp" to "\u00a0",
            "ndash" to "\u2013",
            "mdash" to "\u2014",
            "hellip" to "\u2026",
            "lsquo" to "\u2018",
            "rsquo" to "\u2019",
            "ldquo" to "\u201c",
            "rdquo" to "\u201d",
            "bull" to "\u2022",
            "middot" to "\u00b7",
            "laquo" to "\u00ab",
            "raquo" to "\u00bb",
            "copy" to "\u00a9",
            "reg" to "\u00ae",
            "trade" to "\u2122",
        )
    }
}
