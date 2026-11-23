package com.zigzag.rssreader.feature.reader.presentation

/**
 * Wraps feed-provided article HTML in a document that constrains media to the
 * WebView width. Many feeds ship images with fixed pixel widths, which otherwise
 * overflow the screen and force horizontal scrolling.
 */
internal fun prepareArticleHtml(content: String): String {
    val body = content.replace(DOCUMENT_TAGS, "")
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          html, body { margin: 0; padding: 0; }
          body { line-height: 1.5; overflow-wrap: break-word; padding: 0 16px; }
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
