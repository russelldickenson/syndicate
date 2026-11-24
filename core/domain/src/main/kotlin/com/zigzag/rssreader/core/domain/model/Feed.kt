package com.zigzag.rssreader.core.domain.model

import kotlinx.datetime.Instant

data class FeedPreview(
    val url: String,
    val title: String,
    val siteUrl: String? = null,
    val description: String? = null,
)

data class Feed(
    val id: String,
    val url: String,
    val title: String,
    val siteUrl: String? = null,
    val description: String? = null,
    val lastRefreshAt: Instant? = null,
    val lastError: String? = null,
    val tags: List<Tag> = emptyList(),
    val unreadCount: Int = 0,
)

data class Tag(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val unreadCount: Int = 0,
)

data class Article(
    val id: String,
    val feedId: String,
    val feedTitle: String,
    val feedSiteUrl: String? = null,
    val title: String,
    val link: String,
    val author: String? = null,
    val summary: String? = null,
    val contentHtml: String? = null,
    val publishedAt: Instant? = null,
    val fetchedAt: Instant,
    val isRead: Boolean = false,
)

data class UnreadCounts(
    val global: Int = 0,
    val perFeed: Map<String, Int> = emptyMap(),
    val perTag: Map<String, Int> = emptyMap(),
)

data class RefreshSummary(
    val succeeded: Int = 0,
    val failed: Int = 0,
    val newArticles: Int = 0,
    val errors: List<FeedRefreshError> = emptyList(),
)

data class FeedRefreshError(
    val feedId: String,
    val feedTitle: String,
    val message: String,
)

sealed interface ArticleScope {
    data object All : ArticleScope
    data object Unread : ArticleScope
    data class ForFeed(val feedId: String) : ArticleScope
    data class ForTag(val tagId: String) : ArticleScope
}

enum class ArticleSort(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    TITLE("Title A-Z"),
    FEED("Feed name"),
}

enum class FeedSort(val label: String) {
    NAME("Name"),
    UNREAD("Unread count"),
    LAST_UPDATED("Last updated"),
}

enum class TagSort(val label: String) {
    NAME("Name"),
    UNREAD("Unread count"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class ArticleFont(val label: String) {
    SANS_SERIF("Sans Serif (System)"),
    SERIF("Serif (System)"),
    INTER("Google Inter"),
    ATKINSON_SANS("Atkinson Hyperlegible Next (Sans serif)"),
    ATKINSON_SERIF("Atkinson Hyperlegible Next (Serif)")
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val articleFont: ArticleFont = ArticleFont.SANS_SERIF,
    val refreshOnStartup: Boolean = true,
    val articleSort: ArticleSort = ArticleSort.NEWEST,
    val feedSort: FeedSort = FeedSort.NAME,
    val tagSort: TagSort = TagSort.NAME,
)
