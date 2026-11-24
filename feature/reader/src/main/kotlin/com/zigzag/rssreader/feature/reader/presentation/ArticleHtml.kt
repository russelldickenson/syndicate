package com.zigzag.rssreader.feature.reader.presentation

import com.zigzag.rssreader.core.domain.model.ArticleFont

/**
 * Wraps feed-provided article HTML in a document that constrains media to the
 * WebView width and keeps the text on the app's background. Many feeds ship
 * images with fixed pixel widths, which otherwise overflow the screen and
 * force horizontal scrolling.
 *
 * [embeddedFont] is a Base64-encoded TTF for the two custom font choices; it is
 * inlined as a data URI so it works with `loadDataWithBaseURL`.
 */
internal fun prepareArticleHtml(
    content: String,
    dark: Boolean = false,
    font: ArticleFont = ArticleFont.SANS_SERIF,
    embeddedFont: String? = null,
): String {
    val body = content.replace(DOCUMENT_TAGS, "")
    val textColor = if (dark) "#E3E1EC" else "#1A1C23"
    val linkColor = if (dark) "#B4C0FF" else "#4655A4"
    val fontFace = if (embeddedFont != null) {
        """
        @font-face {
          font-family: 'ArticleFont';
          src: url(data:font/ttf;base64,$embeddedFont) format('truetype');
        }
        """
    } else {
        ""
    }
    val fontFamily = when (font) {
        ArticleFont.SANS_SERIF -> "sans-serif"
        ArticleFont.SERIF -> "serif"
        ArticleFont.INTER, ArticleFont.ATKINSON_SANS -> "'ArticleFont', sans-serif"
        ArticleFont.ATKINSON_SERIF -> "'ArticleFont', serif"
    }
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          $fontFace
          html, body { margin: 0; padding: 0; background: transparent; }
          body {
            padding: 0 0 24px;
            color: $textColor;
            font-family: $fontFamily;
            font-size: 16px;
            line-height: 1.6;
            overflow-wrap: break-word;
          }
          a { color: $linkColor; }
          h1 { font-size: 1.75em; }
          h2 { font-size: 1.4em; }
          h3 { font-size: 1.15em; }
          img { max-width: 100% !important; height: auto !important; }
          video, iframe { max-width: 100%; }
          table { max-width: 100%; }
          pre { white-space: pre-wrap; overflow-x: auto; }
        </style>
        </head>
        <body>$body</body>
        </html>
    """.trimIndent()
}

private val DOCUMENT_TAGS = Regex("""</?(?:html|head|body)\b[^>]*>""", RegexOption.IGNORE_CASE)
