package com.zigzag.rssreader.core.data.mapper

import com.zigzag.rssreader.core.database.dao.FeedRow
import com.zigzag.rssreader.core.database.dao.TagRow
import com.zigzag.rssreader.core.database.model.ArticleEntity
import com.zigzag.rssreader.core.database.model.FeedEntity
import com.zigzag.rssreader.core.database.model.TagEntity
import com.zigzag.rssreader.core.domain.model.Article
import com.zigzag.rssreader.core.domain.model.Feed
import com.zigzag.rssreader.core.domain.model.Tag
import com.zigzag.rssreader.core.network.model.ParsedArticle
import kotlin.time.Clock
import kotlinx.datetime.Instant

internal fun FeedEntity.toDomain(tags: List<Tag> = emptyList(), unreadCount: Int = 0): Feed = Feed(
    id = id,
    url = url,
    title = title,
    siteUrl = siteUrl,
    description = description,
    lastRefreshAt = lastRefreshAt?.let { Instant.fromEpochMilliseconds(it) },
    lastError = lastError,
    tags = tags,
    unreadCount = unreadCount,
)

internal fun FeedRow.toDomain(tags: List<Tag>, unreadCount: Int): Feed = Feed(
    id = id,
    url = url,
    title = title,
    siteUrl = siteUrl,
    description = description,
    lastRefreshAt = lastRefreshAt?.let { Instant.fromEpochMilliseconds(it) },
    lastError = lastError,
    tags = tags,
    unreadCount = unreadCount,
)

internal fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    sortOrder = sortOrder,
)

internal fun TagRow.toDomain(): Tag = Tag(
    id = id,
    name = name,
    sortOrder = sortOrder,
    unreadCount = unreadCount,
)

internal fun ArticleEntity.toDomain(feedTitle: String): Article = Article(
    id = id,
    feedId = feedId,
    feedTitle = feedTitle,
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
