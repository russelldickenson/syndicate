package com.zigzag.rssreader.feature.articles.presentation

import com.zigzag.rssreader.core.domain.model.Article
import com.zigzag.rssreader.core.domain.model.ArticleScope
import com.zigzag.rssreader.core.domain.model.ArticleSort

data class ArticlesUiState(
    val articles: List<Article> = emptyList(),
    val scope: ArticleScope = ArticleScope.All,
    val scopeTitle: String = "All articles",
    val sort: ArticleSort = ArticleSort.NEWEST,
    val unreadOnly: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)
