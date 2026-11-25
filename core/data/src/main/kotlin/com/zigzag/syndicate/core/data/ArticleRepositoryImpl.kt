package com.zigzag.syndicate.core.data

import com.zigzag.syndicate.core.common.DispatcherProvider
import com.zigzag.syndicate.core.data.mapper.toDomain
import com.zigzag.syndicate.core.database.dao.ArticleDao
import com.zigzag.syndicate.core.database.dao.FeedDao
import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.domain.model.ArticleScope
import com.zigzag.syndicate.core.domain.model.ArticleSort
import com.zigzag.syndicate.core.domain.repository.ArticleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

private val ArticleScope.queryKind: String
    get() = when (this) {
        ArticleScope.All -> "ALL"
        ArticleScope.Unread -> "UNREAD"
        is ArticleScope.ForFeed -> "FEED"
        is ArticleScope.ForFolder -> "FOLDER"
    }

private val ArticleScope.queryId: String
    get() = when (this) {
        is ArticleScope.ForFeed -> feedId
        is ArticleScope.ForFolder -> folderId
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
        val feeds = feedTitles.associateBy { it.id }
        val articles = entities.map { entity ->
            val feed = feeds[entity.feedId]
            entity.toDomain(
                feedTitle = feed?.title.orEmpty(),
                feedSiteUrl = feed?.let { it.siteUrl ?: it.url },
            )
        }
        sortArticles(articles, sort)
    }

    override fun observeArticle(articleId: String): Flow<Article?> = combine(
        articleDao.observeArticle(articleId),
        feedDao.observeFeedTitles(),
    ) { entity, feedTitles ->
        val feed = feedTitles.firstOrNull { it.id == entity?.feedId }
        entity?.toDomain(
            feedTitle = feed?.title.orEmpty(),
            feedSiteUrl = feed?.let { it.siteUrl ?: it.url },
        )
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
