package com.zigzag.syndicate.core.data.mapper

import com.zigzag.syndicate.core.database.dao.FeedRow
import com.zigzag.syndicate.core.database.dao.FolderRow
import com.zigzag.syndicate.core.database.model.ArticleEntity
import com.zigzag.syndicate.core.database.model.FeedEntity
import com.zigzag.syndicate.core.database.model.FolderEntity
import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.domain.model.Feed
import com.zigzag.syndicate.core.domain.model.Folder
import com.zigzag.syndicate.core.network.model.ParsedArticle
import kotlin.time.Clock
import kotlinx.datetime.Instant

internal fun FeedEntity.toDomain(unreadCount: Int = 0): Feed = Feed(
    id = id,
    url = url,
    title = title,
    siteUrl = siteUrl,
    description = description,
    lastRefreshAt = lastRefreshAt?.let { Instant.fromEpochMilliseconds(it) },
    lastError = lastError,
    folderId = folderId,
    unreadCount = unreadCount,
)

internal fun FeedRow.toDomain(unreadCount: Int): Feed = Feed(
    id = id,
    url = url,
    title = title,
    siteUrl = siteUrl,
    description = description,
    lastRefreshAt = lastRefreshAt?.let { Instant.fromEpochMilliseconds(it) },
    lastError = lastError,
    folderId = folderId,
    unreadCount = unreadCount,
)

internal fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    sortOrder = sortOrder,
)

internal fun FolderRow.toDomain(): Folder = Folder(
    id = id,
    name = name,
    sortOrder = sortOrder,
    unreadCount = unreadCount,
)

internal fun ArticleEntity.toDomain(
    feedTitle: String,
    feedSiteUrl: String? = null,
): Article = Article(
    id = id,
    feedId = feedId,
    feedTitle = feedTitle,
    feedSiteUrl = feedSiteUrl,
    title = title,
    link = link,
    author = author,
    summary = summary,
    contentHtml = contentHtml,
    publishedAt = publishedAt?.let { Instant.fromEpochMilliseconds(it) },
    fetchedAt = Instant.fromEpochMilliseconds(fetchedAt),
    isRead = isRead,
)

internal fun ParsedArticle.toEntity(
    feedId: String,
    fetchedAt: Long = Clock.System.now().toEpochMilliseconds(),
): ArticleEntity = ArticleEntity(
    id = "$feedId::${id.ifBlank { link }}",
    feedId = feedId,
    title = title,
    link = link,
    author = author,
    summary = summary,
    contentHtml = contentHtml,
    publishedAt = publishedAt?.toEpochMilliseconds(),
    fetchedAt = fetchedAt,
    isRead = false,
)
