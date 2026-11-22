package com.zigzag.rssreader.core.database.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.zigzag.rssreader.core.database.RssDatabase
import com.zigzag.rssreader.core.database.dao.ArticleDao
import com.zigzag.rssreader.core.database.dao.FeedDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RssDatabase =
        Room.databaseBuilder(context, RssDatabase::class.java, "rss-reader.db")
            .setDriver(BundledSQLiteDriver())
            .build()

    @Provides
    fun provideFeedDao(database: RssDatabase): FeedDao = database.feedDao()

    @Provides
    fun provideArticleDao(database: RssDatabase): ArticleDao = database.articleDao()
}
