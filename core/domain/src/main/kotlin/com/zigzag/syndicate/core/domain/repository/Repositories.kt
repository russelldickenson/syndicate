package com.zigzag.syndicate.core.domain.repository

import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.domain.model.ArticleScope
import com.zigzag.syndicate.core.domain.model.ArticleSort
import com.zigzag.syndicate.core.domain.model.Feed
import com.zigzag.syndicate.core.domain.model.FeedImportSummary
import com.zigzag.syndicate.core.domain.model.FeedPreview
import com.zigzag.syndicate.core.domain.model.FeedSort
import com.zigzag.syndicate.core.domain.model.RefreshSummary
import com.zigzag.syndicate.core.domain.model.Folder
import com.zigzag.syndicate.core.domain.model.FolderSort
import com.zigzag.syndicate.core.domain.model.UnreadCounts
import com.zigzag.syndicate.core.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun observeFeeds(sort: FeedSort): Flow<List<Feed>>
    fun observeFolders(sort: FolderSort): Flow<List<Folder>>    fun observeUnreadCounts(): Flow<UnreadCounts>
    suspend fun previewFeed(url: String): Result<FeedPreview>
    suspend fun addFeed(preview: FeedPreview): Result<Feed>
    suspend fun removeFeed(feedId: String)
    suspend fun setFeedFolder(feedId: String, folderId: String?)
    suspend fun addFolder(name: String): Result<Folder>
    suspend fun renameFolder(folderId: String, name: String)
    suspend fun removeFolder(folderId: String)
    suspend fun refreshAll(): RefreshSummary
    suspend fun refreshFeed(feedId: String): Result<Unit>
    suspend fun exportSubscriptions(): Result<String>
    suspend fun importSubscriptions(opml: String): Result<FeedImportSummary>
}

interface ArticleRepository {
    fun observeArticles(
        scope: ArticleScope,
        unreadOnly: Boolean,
        sort: ArticleSort,
    ): Flow<List<Article>>

    fun observeArticle(articleId: String): Flow<Article?>
    suspend fun markRead(articleId: String)
    suspend fun markUnread(articleId: String)
    suspend fun markAllRead(scope: ArticleScope)
}

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings)
}

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
