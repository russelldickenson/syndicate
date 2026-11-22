package com.zigzag.rssreader.core.data.di

import com.zigzag.rssreader.core.common.DefaultDispatcherProvider
import com.zigzag.rssreader.core.common.DispatcherProvider
import com.zigzag.rssreader.core.data.ArticleRepositoryImpl
import com.zigzag.rssreader.core.data.FeedRepositoryImpl
import com.zigzag.rssreader.core.domain.repository.ArticleRepository
import com.zigzag.rssreader.core.domain.repository.FeedRepository
import com.zigzag.rssreader.core.domain.repository.NetworkMonitor
import com.zigzag.rssreader.core.network.ConnectivityMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindFeedRepository(impl: FeedRepositoryImpl): FeedRepository

    @Binds
    @Singleton
    abstract fun bindArticleRepository(impl: ArticleRepositoryImpl): ArticleRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}
