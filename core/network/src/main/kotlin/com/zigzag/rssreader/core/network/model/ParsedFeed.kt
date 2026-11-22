package com.zigzag.rssreader.core.network.model

import kotlinx.datetime.Instant

data class ParsedFeed(
    val title: String,
    val siteUrl: String?,
    val description: String?,
    val articles: List<ParsedArticle>,
)

data class ParsedArticle(
    val id: String,
    val title: String,
    val link: String,
    val author: String?,
    val summary: String?,
    val contentHtml: String?,
    val publishedAt: Instant?,
)
