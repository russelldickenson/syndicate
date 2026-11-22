package com.zigzag.rssreader.core.database.model

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "feeds",
    indices = [Index(value = ["url"], unique = true)],
)
data class FeedEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val siteUrl: String? = null,
    val description: String? = null,
    val lastRefreshAt: Long? = null,
    val lastError: String? = null,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "feed_tags",
    primaryKeys = ["feedId", "tagId"],
    indices = [Index(value = ["tagId"])],
)
data class FeedTagCrossRef(
    val feedId: String,
    val tagId: String,
)

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["feedId"]),
        Index(value = ["isRead"]),
        Index(value = ["publishedAt"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey
    val id: String,
    val feedId: String,
    val title: String,
    val link: String,
    val author: String? = null,
    val summary: String? = null,
    val contentHtml: String? = null,
    val publishedAt: Long? = null,
    val fetchedAt: Long,
    val isRead: Boolean = false,
)
