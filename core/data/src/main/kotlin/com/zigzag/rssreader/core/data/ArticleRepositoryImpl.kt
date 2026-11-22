package com.zigzag.rssreader.core.data

import com.zigzag.rssreader.core.common.DispatcherProvider
import com.zigzag.rssreader.core.data.mapper.toDomain
import com.zigzag.rssreader.core.database.dao.ArticleDao
import com.zigzag.rssreader.core.database.dao.FeedDao
import com.zigzag.rssreader.core.domain.model.Article
import com.zigzag.rssreader.core.domain.model.ArticleScope
import com.zigzag.rssreader.core.domain.model.ArticleSort
import com.zigzag.rssreader.core.domain.repository.ArticleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

private val ArticleScope.queryKind: String
    get() = when (this) {
        ArticleScope.All -> "ALL"
        ArticleScope.Unread -> "UNREAD"
        is ArticleScope.ForFeed -> "FEED"
        is ArticleScope.ForTag -> "TAG"
    }

private val ArticleScope.queryId: String
    get() = when (this) {
        is ArticleScope.ForFeed -> feedId
        is ArticleScope.ForTag -> tagId
        else -> ""
    }

class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao,
    private val dispatchers: DispatcherProvider,
) : ArticleRepository {
    override fun observeArticles(
        scope: ArticleScope,
        unreadOnly: Boolean,
        sort: ArticleSort,
    ): Flow<List<Article>> = combine(
        articleDao.observeArticles(
            scopeKind = scope.queryKind,
            scopeId = scope.queryId,
            unreadOnly = if (unreadOnly) 1 else 0,
            sortKey = sort.name,
        ),
        feedDao.observeFeedTitles(),
    ) { entities, feedTitles ->
        val titles = feedTitles.associate { it.id to it.title }
        val articles = entities.map { entity ->
            entity.toDomain(titles[entity.feedId].orEmpty())
        }
        sortArticles(articles, sort)
    }

    override fun observeArticle(articleId: String): Flow<Article?> = combine(
        articleDao.observeArticle(articleId),
        feedDao.observeFeedTitles(),
    ) { entity, feedTitles ->
        entity?.toDomain(feedTitles.firstOrNull { it.id == entity.feedId }?.title.orEmpty())
    }

    override suspend fun markRead(articleId: String) {
        withContext(dispatchers.io) {
            articleDao.setRead(articleId, true)
        }
    }

    override suspend fun markUnread(articleId: String) {
        withContext(dispatchers.io) {
            articleDao.setRead(articleId, false)
        }
    }

    override suspend fun markAllRead(scope: ArticleScope) {
        withContext(dispatchers.io) {
            articleDao.markAllRead(scope.queryKind, scope.queryId)
        }
    }

    private fun sortArticles(articles: List<Article>, sort: ArticleSort): List<Article> =
        when (sort) {
            ArticleSort.NEWEST -> articles.sortedByDescending { it.publishedAt ?: it.fetchedAt }
            ArticleSort.OLDEST -> articles.sortedBy { it.publishedAt ?: it.fetchedAt }
            ArticleSort.TITLE -> articles.sortedBy { it.title.lowercase() }
            ArticleSort.FEED -> articles.sortedWith(
                compareBy<Article> { it.feedTitle.lowercase() }
                    .thenByDescending { it.publishedAt ?: it.fetchedAt },
            )
        }
}
