package com.zigzag.rssreader.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.zigzag.rssreader.core.database.model.FeedEntity
import com.zigzag.rssreader.core.database.model.FeedTagCrossRef
import com.zigzag.rssreader.core.database.model.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Flattened feed + tag row. Feeds with no tags still produce one row with null tag columns.
 */
data class FeedRow(
    val id: String,
    val url: String,
    val title: String,
    val siteUrl: String?,
    val description: String?,
    val lastRefreshAt: Long?,
    val lastError: String?,
    val unreadCount: Int,
    val tagId: String?,
    val tagName: String?,
    val tagSortOrder: Int?,
    val tagUnreadCount: Int?,
)

data class TagRow(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val unreadCount: Int,
)

data class FeedUnreadRow(
    val feedId: String,
    val count: Int,
)

data class TagUnreadRow(
    val tagId: String,
    val count: Int,
)

data class FeedTitleRow(
    val id: String,
    val title: String,
    val siteUrl: String?,
    val url: String,
)

@Dao
interface FeedDao {
    @Query(
        """
        SELECT
            f.id AS id,
            f.url AS url,
            f.title AS title,
            f.siteUrl AS siteUrl,
            f.description AS description,
            f.lastRefreshAt AS lastRefreshAt,
            f.lastError AS lastError,
            (SELECT COUNT(*) FROM articles a
             WHERE a.feedId = f.id AND a.isRead = 0) AS unreadCount,
            t.id AS tagId,
            t.name AS tagName,
            t.sortOrder AS tagSortOrder,
            (SELECT COUNT(*) FROM feed_tags ft2
             INNER JOIN articles a2 ON a2.feedId = ft2.feedId
             WHERE ft2.tagId = t.id AND a2.isRead = 0) AS tagUnreadCount
        FROM feeds f
        LEFT JOIN feed_tags ft ON ft.feedId = f.id
        LEFT JOIN tags t ON t.id = ft.tagId
        ORDER BY
            CASE WHEN :sortKey = 'NAME' THEN LOWER(f.title) END ASC,
            CASE WHEN :sortKey = 'UNREAD' THEN
                (SELECT COUNT(*) FROM articles a3
                 WHERE a3.feedId = f.id AND a3.isRead = 0) END DESC,
            CASE WHEN :sortKey = 'LAST_UPDATED' THEN f.lastRefreshAt END DESC,
            f.id ASC
        """,
    )
    fun observeFeeds(sortKey: String): Flow<List<FeedRow>>

    @Query(
        """
        SELECT
            t.id AS id,
            t.name AS name,
            t.sortOrder AS sortOrder,
            (SELECT COUNT(*) FROM feed_tags ft
             INNER JOIN articles a ON a.feedId = ft.feedId
             WHERE ft.tagId = t.id AND a.isRead = 0) AS unreadCount
        FROM tags t
        ORDER BY
            CASE WHEN :sortKey = 'NAME' THEN LOWER(t.name) END ASC,
            CASE WHEN :sortKey = 'UNREAD' THEN
                (SELECT COUNT(*) FROM feed_tags ft2
                 INNER JOIN articles a2 ON a2.feedId = ft2.feedId
                 WHERE ft2.tagId = t.id AND a2.isRead = 0) END DESC,
            t.sortOrder ASC,
            t.id ASC
        """,
    )
    fun observeTags(sortKey: String): Flow<List<TagRow>>

    @Query("SELECT COUNT(*) FROM articles WHERE isRead = 0")
    fun observeGlobalUnread(): Flow<Int>

    @Query("SELECT feedId AS feedId, COUNT(*) AS count FROM articles WHERE isRead = 0 GROUP BY feedId")
    fun observeFeedUnread(): Flow<List<FeedUnreadRow>>

    @Query(
        """
        SELECT ft.tagId AS tagId, COUNT(*) AS count
        FROM feed_tags ft
        INNER JOIN articles a ON a.feedId = ft.feedId
        WHERE a.isRead = 0
        GROUP BY ft.tagId
        """,
    )
    fun observeTagUnread(): Flow<List<TagUnreadRow>>

    @Query("SELECT id AS id, title AS title, siteUrl AS siteUrl, url AS url FROM feeds")
    fun observeFeedTitles(): Flow<List<FeedTitleRow>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun feedById(id: String): FeedEntity?

    @Query("SELECT * FROM feeds WHERE url = :url")
    suspend fun feedByUrl(url: String): FeedEntity?

    @Query("SELECT * FROM feeds")
    suspend fun allFeeds(): List<FeedEntity>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun tagById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun tagByName(name: String): TagEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tags")
    suspend fun maxTagSortOrder(): Int

    @Upsert
    suspend fun upsertFeeds(feeds: List<FeedEntity>)

    @Upsert
    suspend fun upsertTag(tag: TagEntity)

    @Query("UPDATE tags SET name = :name WHERE id = :id")
    suspend fun updateTagName(id: String, name: String)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteFeed(id: String)

    @Query("DELETE FROM feed_tags WHERE feedId = :feedId")
    suspend fun deleteFeedTags(feedId: String)

    @Query("DELETE FROM feed_tags WHERE feedId = :feedId")
    suspend fun deleteAllLinksForFeed(feedId: String)

    @Query("DELETE FROM feed_tags WHERE tagId = :tagId")
    suspend fun deleteTagLinks(tagId: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<FeedTagCrossRef>)
}
