package com.zigzag.rssreader.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.zigzag.rssreader.core.database.model.FeedEntity
import com.zigzag.rssreader.core.database.model.FolderEntity
import kotlinx.coroutines.flow.Flow

/** A feed row; [folderId] is null when the feed is not in a folder. */
data class FeedRow(
    val id: String,
    val url: String,
    val title: String,
    val siteUrl: String?,
    val description: String?,
    val lastRefreshAt: Long?,
    val lastError: String?,
    val unreadCount: Int,
    val folderId: String?,
)

data class FolderRow(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val unreadCount: Int,
)

data class FeedUnreadRow(
    val feedId: String,
    val count: Int,
)

data class FolderUnreadRow(
    val folderId: String,
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
            f.folderId AS folderId
        FROM feeds f
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
            fo.id AS id,
            fo.name AS name,
            fo.sortOrder AS sortOrder,
            (SELECT COUNT(*) FROM feeds f
             INNER JOIN articles a ON a.feedId = f.id
             WHERE f.folderId = fo.id AND a.isRead = 0) AS unreadCount
        FROM folders fo
        ORDER BY
            CASE WHEN :sortKey = 'NAME' THEN LOWER(fo.name) END ASC,
            CASE WHEN :sortKey = 'UNREAD' THEN
                (SELECT COUNT(*) FROM feeds f2
                 INNER JOIN articles a2 ON a2.feedId = f2.id
                 WHERE f2.folderId = fo.id AND a2.isRead = 0) END DESC,
            fo.sortOrder ASC,
            fo.id ASC
        """,
    )
    fun observeFolders(sortKey: String): Flow<List<FolderRow>>

    @Query("SELECT COUNT(*) FROM articles WHERE isRead = 0")
    fun observeGlobalUnread(): Flow<Int>

    @Query("SELECT feedId AS feedId, COUNT(*) AS count FROM articles WHERE isRead = 0 GROUP BY feedId")
    fun observeFeedUnread(): Flow<List<FeedUnreadRow>>

    @Query(
        """
        SELECT f.folderId AS folderId, COUNT(*) AS count
        FROM feeds f
        INNER JOIN articles a ON a.feedId = f.id
        WHERE a.isRead = 0 AND f.folderId IS NOT NULL
        GROUP BY f.folderId
        """,
    )
    fun observeFolderUnread(): Flow<List<FolderUnreadRow>>

    @Query("SELECT id AS id, title AS title, siteUrl AS siteUrl, url AS url FROM feeds")
    fun observeFeedTitles(): Flow<List<FeedTitleRow>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun feedById(id: String): FeedEntity?

    @Query("SELECT * FROM feeds WHERE url = :url")
    suspend fun feedByUrl(url: String): FeedEntity?

    @Query("SELECT * FROM feeds")
    suspend fun allFeeds(): List<FeedEntity>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun folderById(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun folderByName(name: String): FolderEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM folders")
    suspend fun maxFolderSortOrder(): Int

    @Upsert
    suspend fun upsertFeeds(feeds: List<FeedEntity>)

    @Upsert
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun updateFolderName(id: String, name: String)

    @Query("UPDATE feeds SET folderId = :folderId WHERE id = :feedId")
    suspend fun setFeedFolder(feedId: String, folderId: String?)

    @Query("UPDATE feeds SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFeedFolder(folderId: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteFeed(id: String)
}
