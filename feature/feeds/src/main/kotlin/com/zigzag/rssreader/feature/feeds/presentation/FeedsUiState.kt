package com.zigzag.rssreader.feature.feeds.presentation

import com.zigzag.rssreader.core.domain.model.Feed
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.Folder
import com.zigzag.rssreader.core.domain.model.FolderSort
import com.zigzag.rssreader.core.domain.model.UnreadCounts

data class FeedsUiState(
    val feeds: List<Feed> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val unreadCounts: UnreadCounts = UnreadCounts(),
    val sort: FeedSort = FeedSort.NAME,
    val folderSort: FolderSort = FolderSort.NAME,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showAddFeedDialog: Boolean = false,
    val feedUrlInput: String = "",
    val showConfirmFeedDialog: Boolean = false,
    val feedPreviewTitle: String = "",
    val feedPreviewUrl: String = "",
    val duplicateFeedId: String? = null,
    val folderEditorFeedId: String? = null,
    val folderEditorFeedTitle: String = "",
    val newFolderNameInput: String = "",
    val selectedFolderId: String? = null,
    val editingFolderId: String? = null,
    val editingFolderName: String = "",
    val isSaving: Boolean = false,
)
