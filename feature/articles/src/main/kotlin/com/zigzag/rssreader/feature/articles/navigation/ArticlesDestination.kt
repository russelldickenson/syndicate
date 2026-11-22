package com.zigzag.rssreader.feature.articles.navigation

import androidx.navigation3.runtime.NavKey
import com.zigzag.rssreader.core.domain.model.ArticleScope
import kotlinx.serialization.Serializable

@Serializable
data class ArticlesDestination(
    val scope: String,
    val tagId: String? = null,
    val feedId: String? = null,
) : NavKey {

    companion object {
        fun all(): ArticlesDestination = ArticlesDestination(scope = SCOPE_ALL)

        fun unread(): ArticlesDestination = ArticlesDestination(scope = SCOPE_UNREAD)

        fun forTag(id: String): ArticlesDestination =
            ArticlesDestination(scope = SCOPE_TAG, tagId = id)

        fun forFeed(id: String): ArticlesDestination =
            ArticlesDestination(scope = SCOPE_FEED, feedId = id)

        private const val SCOPE_ALL = "all"
        private const val SCOPE_UNREAD = "unread"
        private const val SCOPE_TAG = "tag"
        private const val SCOPE_FEED = "feed"
    }

    fun toScope(): ArticleScope =
        when (scope) {
            SCOPE_UNREAD -> ArticleScope.Unread
            SCOPE_TAG -> tagId?.let(ArticleScope::ForTag) ?: ArticleScope.All
            SCOPE_FEED -> feedId?.let(ArticleScope::ForFeed) ?: ArticleScope.All
            else -> ArticleScope.All
        }
}
