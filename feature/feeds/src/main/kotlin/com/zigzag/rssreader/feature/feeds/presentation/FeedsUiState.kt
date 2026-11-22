package com.zigzag.rssreader.feature.feeds.presentation

import com.zigzag.rssreader.core.domain.model.Feed
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.Tag
import com.zigzag.rssreader.core.domain.model.TagSort
import com.zigzag.rssreader.core.domain.model.UnreadCounts

data class FeedsUiState(
    val feeds: List<Feed> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val unreadCounts: UnreadCounts = UnreadCounts(),
    val sort: FeedSort = FeedSort.NAME,
    val tagSort: TagSort = TagSort.NAME,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showAddFeedDialog: Boolean = false,
    val feedUrlInput: String = "",
    val showConfirmFeedDialog: Boolean = false,
    val feedPreviewTitle: String = "",
    val feedPreviewUrl: String = "",
    val tagEditorFeedId: String? = null,
    val tagEditorFeedTitle: String = "",
    val newTagNameInput: String = "",
    val selectedTagIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
)
