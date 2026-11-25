package com.zigzag.syndicate.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.zigzag.syndicate.core.database.dao.ArticleDao
import com.zigzag.syndicate.core.database.dao.FeedDao
import com.zigzag.syndicate.core.database.model.ArticleEntity
import com.zigzag.syndicate.core.database.model.FeedEntity
import com.zigzag.syndicate.core.database.model.FolderEntity

@Database(
    entities = [
        FeedEntity::class,
        FolderEntity::class,
        ArticleEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class RssDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao
}
