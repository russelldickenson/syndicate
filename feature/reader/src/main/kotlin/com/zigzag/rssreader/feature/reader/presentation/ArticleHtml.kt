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
          html { background: #FBF7EB; }
          body {
            margin: 0; padding: 20px 22px 32px;
            max-width: 680px; margin: 0 auto;
            background: #FBF7EB;
            color: #2A2620;
            font-family: Georgia, 'Times New Roman', serif;
            font-size: 17px;
            line-height: 1.65;
            overflow-wrap: break-word;
          }
          h1, h2, h3 { font-family: Georgia, 'Times New Roman', serif; font-weight: 700; line-height: 1.25; }
          a { color: #4655A4; }
          blockquote {
            margin: 18px 0; padding: 4px 0 4px 16px;
            border-left: 3px solid #4655A4;
            color: #5A5348; font-style: italic;
          }
          img { max-width: 100% !important; height: auto !important; border-radius: 6px; }
          video, iframe { max-width: 100%; }
          table { max-width: 100%; }
          pre { white-space: pre-wrap; overflow-x: auto; font-family: monospace; }
        </style>
        </head>
        <body>$body</body>
        </html>
    """.trimIndent()
}

private val DOCUMENT_TAGS = Regex("""</?(?:html|head|body)\b[^>]*>""", RegexOption.IGNORE_CASE)
