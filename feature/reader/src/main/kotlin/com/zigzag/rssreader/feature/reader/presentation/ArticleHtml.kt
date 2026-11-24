package com.zigzag.rssreader.feature.reader.presentation

/**
 * Wraps feed-provided article HTML in a document that constrains media to the
 * WebView width and keeps the text on the app's background. Many feeds ship
 * images with fixed pixel widths, which otherwise overflow the screen and
 * force horizontal scrolling.
 */
internal fun prepareArticleHtml(content: String, dark: Boolean = false): String {
    val body = content.replace(DOCUMENT_TAGS, "")
    val textColor = if (dark) "#E3E1EC" else "#1A1C23"
    val linkColor = if (dark) "#B4C0FF" else "#4655A4"
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          html, body { margin: 0; padding: 0; background: transparent; }
          body {
            padding: 0 0 24px;
            color: $textColor;
            font-size: 16px;
            line-height: 1.6;
            overflow-wrap: break-word;
          }
          a { color: $linkColor; }
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
