package com.zigzag.syndicate.core.data

import com.zigzag.syndicate.core.domain.model.Feed
import com.zigzag.syndicate.core.domain.model.FeedPreview
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader

/** Builds an OPML 2.0 document containing the given feeds. */
internal fun buildOpml(feeds: List<Feed>): String {
    val outlines = feeds.joinToString("\n") { feed ->
        """
        <outline type="rss" text="${escapeXml(feed.title)}" title="${escapeXml(feed.title)}" xmlUrl="${escapeXml(feed.url)}" htmlUrl="${escapeXml(feed.siteUrl.orEmpty())}" />"""
    }
    return """<?xml version="1.0" encoding="UTF-8"?>
<opml version="2.0">
  <head><title>Syndicate subscriptions</title></head>
  <body>
$outlines
  </body>
</opml>""".trimIndent()
}

/** Reads feed URLs from an OPML document, ignoring outlines without an xmlUrl. */
internal fun parseOpml(opml: String): List<FeedPreview> {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isExpandEntityReferences = false
        runCatching { setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching {
            setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
            setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
        }
    }
    val document = factory.newDocumentBuilder().parse(InputSource(StringReader(opml)))
    val result = mutableListOf<FeedPreview>()
    val outlines = document.getElementsByTagName("outline")
    for (i in 0 until outlines.length) {
        val element = outlines.item(i) as? Element ?: continue
        val url = element.getAttribute("xmlUrl").trim()
        if (url.isEmpty()) continue
        val title = element.getAttribute("title")
            .ifBlank { element.getAttribute("text") }
            .ifBlank { url }
        result += FeedPreview(
            url = url,
            title = title,
            siteUrl = element.getAttribute("htmlUrl").takeIf { it.isNotBlank() },
        )
    }
    return result
}

private fun escapeXml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")
