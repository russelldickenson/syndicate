package com.zigzag.syndicate.feature.articles.presentation

import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.domain.model.ArticleScope
import com.zigzag.syndicate.core.domain.model.ArticleSort

data class ArticlesUiState(
    val articles: List<Article> = emptyList(),
    val scope: ArticleScope = ArticleScope.All,
    val scopeTitle: String = "All articles",
    val sort: ArticleSort = ArticleSort.NEWEST,
    val unreadOnly: Boolean = false,
    val showConfirmMarkAllRead: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)
