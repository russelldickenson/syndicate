package com.zigzag.syndicate.feature.articles.navigation

import androidx.navigation3.runtime.NavKey
import com.zigzag.syndicate.core.domain.model.ArticleScope
import kotlinx.serialization.Serializable

@Serializable
data class ArticlesDestination(
    val scope: String,
    val folderId: String? = null,
    val feedId: String? = null,
) : NavKey {

    companion object {
        fun all(): ArticlesDestination = ArticlesDestination(scope = SCOPE_ALL)

        fun unread(): ArticlesDestination = ArticlesDestination(scope = SCOPE_UNREAD)

        fun forFolder(id: String): ArticlesDestination =
            ArticlesDestination(scope = SCOPE_FOLDER, folderId = id)

        fun forFeed(id: String): ArticlesDestination =
            ArticlesDestination(scope = SCOPE_FEED, feedId = id)

        private const val SCOPE_ALL = "all"
        private const val SCOPE_UNREAD = "unread"
        private const val SCOPE_FOLDER = "folder"
        private const val SCOPE_FEED = "feed"
    }

    fun toScope(): ArticleScope =
        when (scope) {
            SCOPE_UNREAD -> ArticleScope.Unread
            SCOPE_FOLDER -> folderId?.let(ArticleScope::ForFolder) ?: ArticleScope.All
            SCOPE_FEED -> feedId?.let(ArticleScope::ForFeed) ?: ArticleScope.All
            else -> ArticleScope.All
        }
}
