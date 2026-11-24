package com.zigzag.rssreader.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.zigzag.rssreader.core.database.dao.ArticleDao
import com.zigzag.rssreader.core.database.dao.FeedDao
import com.zigzag.rssreader.core.database.model.ArticleEntity
import com.zigzag.rssreader.core.database.model.FeedEntity
import com.zigzag.rssreader.core.database.model.FolderEntity

@Database(
    entities = [
        FeedEntity::class,
        FolderEntity::class,
        ArticleEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class RssDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao
}
