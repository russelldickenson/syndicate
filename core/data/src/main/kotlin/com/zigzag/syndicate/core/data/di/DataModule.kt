package com.zigzag.syndicate.core.data.di

import com.zigzag.syndicate.core.common.DefaultDispatcherProvider
import com.zigzag.syndicate.core.common.DispatcherProvider
import com.zigzag.syndicate.core.data.ArticleRepositoryImpl
import com.zigzag.syndicate.core.data.FeedRepositoryImpl
import com.zigzag.syndicate.core.domain.repository.ArticleRepository
import com.zigzag.syndicate.core.domain.repository.FeedRepository
import com.zigzag.syndicate.core.domain.repository.NetworkMonitor
import com.zigzag.syndicate.core.network.ConnectivityMonitor
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
