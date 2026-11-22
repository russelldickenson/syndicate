package com.zigzag.rssreader.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.zigzag.rssreader.core.database.model.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query(
        """
        SELECT a.* FROM articles a
        INNER JOIN feeds f ON f.id = a.feedId
        WHERE
            (
                :scopeKind = 'ALL'
                OR (:scopeKind = 'UNREAD' AND a.isRead = 0)
                OR (:scopeKind = 'FEED' AND a.feedId = :scopeId)
                OR (:scopeKind = 'TAG' AND a.feedId IN (
                    SELECT ft.feedId FROM feed_tags ft WHERE ft.tagId = :scopeId
                ))
            )
            AND (:unreadOnly = 0 OR a.isRead = 0)
        ORDER BY
            CASE WHEN :sortKey = 'NEWEST' THEN COALESCE(a.publishedAt, a.fetchedAt) END DESC,
            CASE WHEN :sortKey = 'OLDEST' THEN COALESCE(a.publishedAt, a.fetchedAt) END ASC,
            CASE WHEN :sortKey = 'TITLE' THEN LOWER(a.title) END ASC,
            CASE WHEN :sortKey = 'FEED' THEN LOWER(f.title) END ASC,
            a.id ASC
        """,
    )
    fun observeArticles(
        scopeKind: String,
        scopeId: String,
        unreadOnly: Int,
        sortKey: String,
    ): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeArticle(id: String): Flow<ArticleEntity?>

    @Query("UPDATE articles SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: String, isRead: Boolean)

    @Query(
        """
        UPDATE articles SET isRead = 1
        WHERE
            :scopeKind = 'ALL'
            OR (:scopeKind = 'UNREAD' AND isRead = 0)
            OR (:scopeKind = 'FEED' AND feedId = :scopeId)
            OR (:scopeKind = 'TAG' AND feedId IN (
                SELECT ft.feedId FROM feed_tags ft WHERE ft.tagId = :scopeId
            ))
        """,
    )
    suspend fun markAllRead(scopeKind: String, scopeId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Upsert
    suspend fun upsertArticles(articles: List<ArticleEntity>)

    @Query("SELECT id FROM articles WHERE feedId = :feedId")
    suspend fun articleIdsByFeed(feedId: String): List<String>

    @Query("SELECT COUNT(*) FROM articles WHERE feedId = :feedId")
    suspend fun countByFeedId(feedId: String): Int

    @Query("DELETE FROM articles WHERE feedId = :feedId")
    suspend fun deleteArticlesForFeed(feedId: String)
}
