package com.zigzag.rssreader.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedParserTest {

    private val parser = FeedParser()

    @Test
    fun parsesRss2ItemsWithGuidAuthorAndFullContent() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:dc="http://purl.org/dc/elements/1.1/">
              <channel>
                <title>Example &amp; News</title>
                <link>https://example.com</link>
                <description>Daily updates</description>
                <item>
                  <title>First story</title>
                  <link>https://example.com/1</link>
                  <guid>post-1</guid>
                  <dc:creator>Jane Doe</dc:creator>
                  <description>Short &lt;em&gt;summary&lt;/em&gt;</description>
                  <content:encoded><![CDATA[<p>Full <strong>article</strong> body</p>]]></content:encoded>
                  <pubDate>Mon, 23 Nov 2026 07:00:00 +0000</pubDate>
                </item>
                <item>
                  <title>Second story</title>
                  <link>https://example.com/2</link>
                  <description>Another summary</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = parser.parse(xml)

        assertEquals("Example & News", feed.title)
        assertEquals("https://example.com", feed.siteUrl)
        assertEquals("Daily updates", feed.description)
        assertEquals(2, feed.articles.size)
        val first = feed.articles[0]
        assertEquals("post-1", first.id)
        assertEquals("First story", first.title)
        assertEquals("https://example.com/1", first.link)
        assertEquals("Jane Doe", first.author)
        assertEquals("Short <em>summary</em>", first.summary)
        assertEquals("<p>Full <strong>article</strong> body</p>", first.contentHtml)
        assertNotNull(first.publishedAt)
        assertEquals("2026-11-23T07:00:00Z", first.publishedAt.toString())
        assertEquals("https://example.com/2", feed.articles[1].id)
        assertNull(feed.articles[1].publishedAt)
    }

    @Test
    fun parsesAtomEntriesWithAlternateLinksAndUpdatedFallback() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom Journal</title>
              <subtitle>Notes</subtitle>
              <link rel="alternate" href="https://atom.example.com"/>
              <author><name>Editor</name></author>
              <entry>
                <title>Hello Atom</title>
                <id>urn:entry:1</id>
                <link rel="alternate" href="https://atom.example.com/1"/>
                <summary>Summary text</summary>
                <content type="html">&lt;p&gt;Body&lt;/p&gt;</content>
                <published>2026-11-22T10:15:00Z</published>
                <updated>2026-11-23T10:15:00Z</updated>
              </entry>
              <entry>
                <title>Updated only</title>
                <id>urn:entry:2</id>
                <link href="https://atom.example.com/2"/>
                <updated>2026-11-20T08:00:00Z</updated>
              </entry>
            </feed>
        """.trimIndent()

        val feed = parser.parse(xml)

        assertEquals("Atom Journal", feed.title)
        assertEquals("https://atom.example.com", feed.siteUrl)
        assertEquals("Notes", feed.description)
        assertEquals(2, feed.articles.size)
        assertEquals("urn:entry:1", feed.articles[0].id)
        assertEquals("https://atom.example.com/1", feed.articles[0].link)
        assertEquals("Editor", feed.articles[0].author)
        assertEquals("<p>Body</p>", feed.articles[0].contentHtml)
        assertEquals("2026-11-22T10:15:00Z", feed.articles[0].publishedAt.toString())
        assertEquals("2026-11-20T08:00:00Z", feed.articles[1].publishedAt.toString())
    }

    @Test
    fun fallsBackToSummaryWhenFullContentMissing() {
        val xml = """
            <rss version="2.0"><channel><title>Minimal</title>
              <item><title>Only summary</title><link>https://example.com/3</link>
              <description>Just a summary</description></item>
            </channel></rss>
        """.trimIndent()

        val feed = parser.parse(xml)

        assertEquals(1, feed.articles.size)
        assertEquals("Just a summary", feed.articles[0].contentHtml)
        assertTrue(feed.articles[0].link.isNotBlank())
        assertNull(feed.articles[0].publishedAt)
    }
}
