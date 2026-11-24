package com.zigzag.rssreader.feature.reader.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleHtmlTest {

    @Test
    fun constrainsImagesToTheWebViewWidth() {
        val prepared = prepareArticleHtml("""<img src="wide.png" width="1800" height="900">""")

        assertTrue(prepared.contains("img { max-width: 100% !important; height: auto !important; }"))
        assertTrue(prepared.contains("name=\"viewport\""))
        assertTrue(prepared.contains("background: transparent;"))
        assertTrue(prepared.contains("""<img src="wide.png" width="1800" height="900">"""))
    }

    @Test
    fun usesThemeAwareTextColors() {
        assertTrue(prepareArticleHtml("x", dark = false).contains("color: #1A1C23;"))
        assertTrue(prepareArticleHtml("x", dark = true).contains("color: #E3E1EC;"))
    }

    @Test
    fun unwrapsFullHtmlDocumentsIntoTheArticleBody() {
        val prepared = prepareArticleHtml(
            """<html><head><title>Title</title></head><body><p>Text</p><img src="x.png"></body></html""",
        )

        assertTrue(prepared.contains("<p>Text</p><img src=\"x.png\">"))
        assertFalse(prepared.contains("<html><head>"))
        assertTrue(prepared.contains("<title>Title</title><p>Text</p><img src=\"x.png\">"))
    }
}
