package com.zigzag.syndicate.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.domain.model.Feed
import com.zigzag.syndicate.core.domain.model.Folder
import kotlinx.datetime.Instant

private val sampleInstant = Instant.parse("2026-06-23T08:30:00Z")
private val sampleFeed = Feed(id = "feed", url = "https://example.com/feed", title = "Example News")
private val sampleFolder = Folder(id = "folder", name = "Technology", unreadCount = 4)

@Preview(name = "Article row — unread", showBackground = true)
@Composable
private fun ArticleRowUnreadPreview() {
    MaterialTheme {
        ArticleRow(
            article = Article(
                id = "article",
                feedId = sampleFeed.id,
                feedTitle = sampleFeed.title,
                feedSiteUrl = sampleFeed.siteUrl,
                title = "A headline that wraps onto a second line when needed",
                link = "https://example.com/article",
                publishedAt = sampleInstant,
                fetchedAt = sampleInstant,
            ),
            onRead = {},
        )
    }
}

@Preview(name = "Article row — read", showBackground = true)
@Composable
private fun ArticleRowReadPreview() {
    MaterialTheme {
        ArticleRow(
            article = Article(
                id = "article",
                feedId = sampleFeed.id,
                feedTitle = sampleFeed.title,
                title = "A short headline",
                link = "https://example.com/article",
                publishedAt = sampleInstant,
                fetchedAt = sampleInstant,
                isRead = true,
            ),
            onRead = {},
        )
    }
}

@Preview(name = "Feed row", showBackground = true)
@Composable
private fun FeedRowPreview() {
    MaterialTheme {
        FeedRow(feed = sampleFeed.copy(unreadCount = 12), onFeedClick = {}, onEditFolder = {})
    }
}

@Preview(name = "Folder row", showBackground = true)
@Composable
private fun FolderRowPreview() {
    MaterialTheme {
        FolderRow(folder = sampleFolder, onClick = {}, onEdit = {})
    }
}
