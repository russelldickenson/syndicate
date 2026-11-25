package com.zigzag.syndicate.feature.reader.presentation

import com.zigzag.syndicate.core.domain.model.ArticleFont
import com.zigzag.syndicate.core.domain.model.ArticleFontSize
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
    fun usesSmallerHeadingSizes() {
        val prepared = prepareArticleHtml("x")
        assertTrue(prepared.contains("h1 { font-size: 1.75em; }"))
        assertTrue(prepared.contains("h2 { font-size: 1.4em; }"))
        assertTrue(prepared.contains("h3 { font-size: 1.15em; }"))
    }

    @Test
    fun usesSelectedFontSize() {
        assertTrue(prepareArticleHtml("x").contains("font-size: 16px;"))
        assertTrue(prepareArticleHtml("x", fontSize = ArticleFontSize.SMALL).contains("font-size: 14px;"))
        assertTrue(prepareArticleHtml("x", fontSize = ArticleFontSize.LARGE).contains("font-size: 20px;"))
    }

    @Test
    fun usesSystemFontFamilies() {
        assertTrue(prepareArticleHtml("x", font = ArticleFont.SANS_SERIF).contains("font-family: sans-serif;"))
        assertTrue(prepareArticleHtml("x", font = ArticleFont.SERIF).contains("font-family: serif;"))
    }

    @Test
    fun embedsCustomFontsAsDataUris() {
        val prepared = prepareArticleHtml(
            "x",
            font = ArticleFont.INTER,
            embeddedFont = "QUJD",
        )

        assertTrue(prepared.contains("@font-face"))
        assertTrue(prepared.contains("url(data:font/ttf;base64,QUJD)"))
        assertTrue(prepared.contains("font-family: 'ArticleFont', sans-serif;"))
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
