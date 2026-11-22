package com.zigzag.rssreader.core.domain.model

/**
 * Raised when a feed URL is already subscribed to. Carries the existing feed so
 * the UI can offer a link straight to it.
 */
class FeedAlreadyExistsException(
    val feedId: String,
    val feedTitle: String,
) : Exception("This feed has already been added")
