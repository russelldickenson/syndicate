package com.zigzag.rssreader.core.data

import com.zigzag.rssreader.core.common.DispatcherProvider
import com.zigzag.rssreader.core.data.mapper.toDomain
import com.zigzag.rssreader.core.data.mapper.toEntity
import com.zigzag.rssreader.core.database.dao.ArticleDao
import com.zigzag.rssreader.core.database.dao.FeedDao
import com.zigzag.rssreader.core.database.dao.FeedRow
import com.zigzag.rssreader.core.database.model.FeedEntity
import com.zigzag.rssreader.core.database.model.FolderEntity
import com.zigzag.rssreader.core.domain.model.Feed
import com.zigzag.rssreader.core.domain.model.FeedAlreadyExistsException
import com.zigzag.rssreader.core.domain.model.FeedPreview
import com.zigzag.rssreader.core.domain.model.FeedRefreshError
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.RefreshSummary
import com.zigzag.rssreader.core.domain.model.Folder
import com.zigzag.rssreader.core.domain.model.FolderSort
import com.zigzag.rssreader.core.domain.model.UnreadCounts
import com.zigzag.rssreader.core.domain.repository.FeedRepository
import com.zigzag.rssreader.core.network.FeedFetcher
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class FeedRepositoryImpl @Inject constructor(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val fetcher: FeedFetcher,
    private val dispatchers: DispatcherProvider,
) : FeedRepository {
    override fun observeFeeds(sort: FeedSort): Flow<List<Feed>> =
        feedDao.observeFeeds(sort.name).map { rows -> groupFeeds(rows, sort) }

    override fun observeFolders(sort: FolderSort): Flow<List<Folder>> =
        feedDao.observeFolders(sort.name).map { rows ->
            val folders = rows.map { it.toDomain() }
            when (sort) {
                FolderSort.NAME -> folders.sortedBy { it.name.lowercase() }
                FolderSort.UNREAD -> folders.sortedWith(
                    compareByDescending<Folder> { it.unreadCount }.thenBy { it.name.lowercase() },
                )
            }
        }

    override fun observeUnreadCounts(): Flow<UnreadCounts> = combine(
        feedDao.observeGlobalUnread(),
        feedDao.observeFeedUnread(),
        feedDao.observeFolderUnread(),
    ) { global, feedRows, folderRows ->
        UnreadCounts(
            global = global,
            perFeed = feedRows.associate { it.feedId to it.count },
            perFolder = folderRows.associate { it.folderId to it.count },
        )
    }

    override suspend fun previewFeed(url: String): Result<FeedPreview> = withContext(dispatchers.io) {
        runCatching {
            val (normalized, uri) = normalizeFeedUrl(url)
            feedDao.feedByUrl(normalized)?.let { existing ->
                throw FeedAlreadyExistsException(existing.id, existing.title)
            }

            val parsed = fetcher.fetch(normalized)
            FeedPreview(
                url = normalized,
                title = parsed.title.ifBlank { uri.host },
                siteUrl = parsed.siteUrl,
                description = parsed.description,
            )
        }
    }

    override suspend fun addFeed(preview: FeedPreview): Result<Feed> = withContext(dispatchers.io) {
        runCatching {
            val (normalized, uri) = normalizeFeedUrl(preview.url)
            feedDao.feedByUrl(normalized)?.let { existing ->
                throw FeedAlreadyExistsException(existing.id, existing.title)
            }

            val parsed = fetcher.fetch(normalized)
            val id = normalized.hashCode().toString()
            val now = Clock.System.now().toEpochMilliseconds()
            val entity = FeedEntity(
                id = id,
                url = normalized,
                title = preview.title.ifBlank { parsed.title }.ifBlank { uri.host },
                siteUrl = preview.siteUrl ?: parsed.siteUrl,
                description = preview.description ?: parsed.description,
                lastRefreshAt = now,
                lastError = null,
            )
            feedDao.upsertFeeds(listOf(entity))
            articleDao.insertArticles(parsed.articles.map { article -> article.toEntity(id, now) })
            entity.toDomain(unreadCount = parsed.articles.size)
        }
    }

    private fun normalizeFeedUrl(url: String): Pair<String, URI> {
        val trimmed = url.trim()
        val uri = URI(trimmed)
        require(uri.scheme?.lowercase() in setOf("http", "https")) {
            "Feed URL must use http or https"
        }
        require(!uri.host.isNullOrBlank()) { "Feed URL is missing a host" }
        return uri.toString() to uri
    }

    override suspend fun removeFeed(feedId: String) {
        withContext(dispatchers.io) {
            articleDao.deleteArticlesForFeed(feedId)
            feedDao.deleteFeed(feedId)
        }
    }

    override suspend fun setFeedFolder(feedId: String, folderId: String?) {
        withContext(dispatchers.io) {
            feedDao.setFeedFolder(feedId, folderId)
        }
    }

    override suspend fun addFolder(name: String): Result<Folder> = withContext(dispatchers.io) {
        runCatching {
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { "Folder name cannot be blank" }
            require(feedDao.folderByName(trimmed) == null) { "A folder with this name already exists" }
            val entity = FolderEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                sortOrder = feedDao.maxFolderSortOrder() + 1,
            )
            feedDao.upsertFolder(entity)
            entity.toDomain()
        }
    }

    override suspend fun renameFolder(folderId: String, name: String) {
        withContext(dispatchers.io) {
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { "Folder name cannot be blank" }
            val existing = feedDao.folderByName(trimmed)
            require(existing == null || existing.id == folderId) { "A folder with this name already exists" }
            feedDao.updateFolderName(folderId, trimmed)
        }
    }

    override suspend fun removeFolder(folderId: String) {
        withContext(dispatchers.io) {
            feedDao.clearFeedFolder(folderId)
            feedDao.deleteFolder(folderId)
        }
    }

    override suspend fun refreshAll(): RefreshSummary = withContext(dispatchers.io) {
        val feeds = feedDao.allFeeds()
        var succeeded = 0
        var failed = 0
        var newArticles = 0
        val errors = mutableListOf<FeedRefreshError>()

        for (feed in feeds) {
            try {
                newArticles += refreshOne(feed)
                succeeded++
            } catch (exception: Exception) {
                failed++
                val message = exception.message?.takeIf { it.isNotBlank() } ?: "Refresh failed"
                errors += FeedRefreshError(feed.id, feed.title, message)
                recordError(feed.id, message)
            }
        }
        RefreshSummary(
            succeeded = succeeded,
            failed = failed,
            newArticles = newArticles,
            errors = errors,
        )
    }

    override suspend fun refreshFeed(feedId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val feed = feedDao.feedById(feedId) ?: error("Feed not found")
            refreshOne(feed)
            Unit
        }.onFailure { exception ->
            val message = exception.message?.takeIf { it.isNotBlank() } ?: "Refresh failed"
            recordError(feedId, message)
        }
    }

    private suspend fun refreshOne(feed: FeedEntity): Int {
        val parsed = fetcher.fetch(feed.url)
        val now = Clock.System.now().toEpochMilliseconds()
        val existingIds = articleDao.articleIdsByFeed(feed.id).toHashSet()
        val newArticles = parsed.articles
            .map { article -> article.toEntity(feed.id, now) }
            .filter { article -> article.id !in existingIds }
        if (newArticles.isNotEmpty()) {
            articleDao.insertArticles(newArticles)
        }
        feedDao.upsertFeeds(
            listOf(
                feed.copy(
                    title = parsed.title.ifBlank { feed.title },
                    siteUrl = parsed.siteUrl ?: feed.siteUrl,
                    description = parsed.description ?: feed.description,
                    lastRefreshAt = now,
                    lastError = null,
                ),
            ),
        )
        return newArticles.size
    }

    private suspend fun recordError(feedId: String, message: String) {
        runCatching {
            feedDao.feedById(feedId)?.let { current ->
                feedDao.upsertFeeds(listOf(current.copy(lastError = message)))
            }
        }
    }

    private fun groupFeeds(rows: List<FeedRow>, sort: FeedSort): List<Feed> {
        val feeds = rows.map { row -> row.toDomain(unreadCount = row.unreadCount) }
        return when (sort) {
            FeedSort.NAME -> feeds.sortedBy { it.title.lowercase() }
            FeedSort.UNREAD -> feeds.sortedWith(
                compareByDescending<Feed> { it.unreadCount }.thenBy { it.title.lowercase() },
            )
            FeedSort.LAST_UPDATED -> feeds.sortedWith(
                compareByDescending<Feed> { it.lastRefreshAt?.toEpochMilliseconds() ?: 0L }
                    .thenBy { it.title.lowercase() },
            )
        }
    }
}
