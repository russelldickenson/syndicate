package com.zigzag.syndicate.core.domain.model

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
    val folderId: String? = null,
    val unreadCount: Int = 0,
)

data class Folder(
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
    val isFavourite: Boolean = false,
)

data class UnreadCounts(
    val global: Int = 0,
    val perFeed: Map<String, Int> = emptyMap(),
    val perFolder: Map<String, Int> = emptyMap(),
)

data class FeedImportSummary(
    val added: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
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
    data object Favourites : ArticleScope
    data class ForFeed(val feedId: String) : ArticleScope
    data class ForFolder(val folderId: String) : ArticleScope
}

enum class ArticleSort {
    NEWEST, OLDEST, TITLE, FEED,
}

enum class FeedSort {
    NAME, UNREAD, LAST_UPDATED,
}

enum class FolderSort {
    NAME, UNREAD,
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK,
}

enum class ArticleFont {
    SANS_SERIF, SERIF, INTER, ATKINSON,
}

enum class ArticleFontSize {
    SMALL, MEDIUM, LARGE,
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val articleFont: ArticleFont = ArticleFont.SANS_SERIF,
    val articleFontSize: ArticleFontSize = ArticleFontSize.MEDIUM,
    val refreshOnStartup: Boolean = true,
    val articleSort: ArticleSort = ArticleSort.NEWEST,
    val feedSort: FeedSort = FeedSort.NAME,
    val folderSort: FolderSort = FolderSort.NAME,
)