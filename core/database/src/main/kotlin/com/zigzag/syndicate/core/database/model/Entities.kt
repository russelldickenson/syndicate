package com.zigzag.syndicate.core.database.model

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
    val folderId: String? = null,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
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
