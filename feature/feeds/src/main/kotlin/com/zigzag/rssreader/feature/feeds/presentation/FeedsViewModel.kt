package com.zigzag.rssreader.feature.feeds.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zigzag.rssreader.core.domain.model.Feed
import com.zigzag.rssreader.core.domain.model.FeedPreview
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.Tag
import com.zigzag.rssreader.core.domain.model.TagSort
import com.zigzag.rssreader.core.domain.model.UnreadCounts
import com.zigzag.rssreader.core.domain.model.UserSettings
import com.zigzag.rssreader.core.domain.repository.ArticleRepository
import com.zigzag.rssreader.core.domain.repository.FeedRepository
import com.zigzag.rssreader.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedsViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    articleRepository: ArticleRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val feedSort = MutableStateFlow(FeedSort.NAME)
    private val tagSort = MutableStateFlow(TagSort.NAME)
    private val refreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val dialog = MutableStateFlow(FeedsDialogState())

    private val feedsFlow: Flow<List<Feed>> =
        feedSort.flatMapLatest { sort -> feedRepository.observeFeeds(sort) }
    private val tagsFlow: Flow<List<Tag>> =
        tagSort.flatMapLatest { sort -> feedRepository.observeTags(sort) }
    private val unreadCountsFlow: Flow<UnreadCounts> = feedRepository.observeUnreadCounts()

    val uiState: StateFlow<FeedsUiState> =
        combine(
            combine(feedsFlow, tagsFlow, unreadCountsFlow) { feeds, tags, counts ->
                FeedData(feeds, tags, counts)
            },
            combine(feedSort, tagSort) { feed, tag -> SortData(feed, tag) },
            combine(refreshing, errorMessage) { isRefreshing, error -> StatusData(isRefreshing, error) },
            dialog,
        ) { data, sorts, status, dialogState ->
            FeedsUiState(
                feeds = data.feeds,
                tags = data.tags,
                unreadCounts = data.counts,
                sort = sorts.feed,
                tagSort = sorts.tag,
                isRefreshing = status.isRefreshing,
                error = status.error,
                showAddFeedDialog = dialogState.showAddFeed,
                feedUrlInput = dialogState.feedUrlInput,
                showConfirmFeedDialog = dialogState.showConfirmFeed,
                feedPreviewTitle = dialogState.feedPreview?.title.orEmpty(),
                feedPreviewUrl = dialogState.feedPreview?.url.orEmpty(),
                tagEditorFeedId = dialogState.tagEditorFeedId,
                tagEditorFeedTitle = dialogState.tagEditorFeedTitle,
                newTagNameInput = dialogState.newTagNameInput,
                selectedTagIds = dialogState.selectedTagIds,
                isSaving = dialogState.isSaving,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = FeedsUiState(),
        )

    init {
        // Keep the local sort selectors in sync with the persisted preferences (Settings screen).
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                feedSort.value = settings.feedSort
                tagSort.value = settings.tagSort
            }
        }
    }

    fun previewFeed(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            errorMessage.value = "Enter a feed URL."
            return
        }
        dialog.updateCopy { copy(isSaving = true) }
        viewModelScope.launch {
            feedRepository.previewFeed(trimmed).fold(
                onSuccess = { preview ->
                    errorMessage.value = null
                    dialog.updateCopy {
                        copy(
                            showAddFeed = false,
                            showConfirmFeed = true,
                            feedPreview = preview,
                            isSaving = false,
                        )
                    }
                },
                onFailure = { throwable ->
                    errorMessage.value = "Couldn't add feed: ${throwable.message.orUnknown()}"
                    dialog.updateCopy { copy(isSaving = false) }
                },
            )
        }
    }

    fun confirmAddFeed() {
        val preview = dialog.value.feedPreview ?: return
        dialog.updateCopy { copy(isSaving = true) }
        viewModelScope.launch {
            feedRepository.addFeed(preview).fold(
                onSuccess = {
                    errorMessage.value = null
                    dialog.value = FeedsDialogState()
                },
                onFailure = { throwable ->
                    errorMessage.value = "Couldn't add feed: ${throwable.message.orUnknown()}"
                    dialog.updateCopy { copy(isSaving = false) }
                },
            )
        }
    }

    fun backToFeedUrl() {
        errorMessage.value = null
        dialog.updateCopy {
            copy(
                showAddFeed = true,
                showConfirmFeed = false,
                feedPreview = null,
                isSaving = false,
            )
        }
    }

    fun removeFeed(id: String) {
        viewModelScope.launch {
            runCatching { feedRepository.removeFeed(id) }
                .onFailure { errorMessage.value = "Couldn't remove feed: ${it.message.orUnknown()}" }
        }
    }

    fun setFeedTags(feedId: String, tagIds: Set<String>) {
        dialog.updateCopy { copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { feedRepository.setFeedTags(feedId, tagIds) }
                .onSuccess { dialog.updateCopy { copy(tagEditorFeedId = null, selectedTagIds = emptySet(), isSaving = false) } }
                .onFailure {
                    errorMessage.value = "Couldn't update tags: ${it.message.orUnknown()}"
                    dialog.updateCopy { copy(isSaving = false) }
                }
        }
    }

    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            errorMessage.value = "Enter a tag name."
            return
        }
        viewModelScope.launch {
            feedRepository.addTag(trimmed).fold(
                onSuccess = { tag ->
                    dialog.updateCopy {
                        copy(newTagNameInput = "", selectedTagIds = selectedTagIds + tag.id)
                    }
                    errorMessage.value = null
                },
                onFailure = { errorMessage.value = "Couldn't add tag: ${it.message.orUnknown()}" },
            )
        }
    }

    fun renameTag(tagId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            errorMessage.value = "Enter a tag name."
            return
        }
        viewModelScope.launch {
            runCatching { feedRepository.renameTag(tagId, trimmed) }
                .onFailure { errorMessage.value = "Couldn't rename tag: ${it.message.orUnknown()}" }
        }
    }

    fun removeTag(tagId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.removeTag(tagId) }
                .onFailure { errorMessage.value = "Couldn't remove tag: ${it.message.orUnknown()}" }
        }
    }

    fun setFeedSort(sort: FeedSort) {
        feedSort.value = sort
        persistSettings { it.copy(feedSort = sort) }
    }

    fun setTagSort(sort: TagSort) {
        tagSort.value = sort
        persistSettings { it.copy(tagSort = sort) }
    }

    fun refreshAll() {
        if (refreshing.value) return
        refreshing.value = true
        errorMessage.value = null
        viewModelScope.launch {
            runCatching { feedRepository.refreshAll() }
                .onSuccess { summary ->
                    if (summary.failed > 0) {
                        val detail = summary.errors.firstOrNull()?.message
                        errorMessage.value =
                            "Refreshed ${summary.succeeded} feed(s), ${summary.failed} failed" +
                                detail?.let { ": $it" }.orEmpty()
                    }
                }
                .onFailure { errorMessage.value = "Refresh failed: ${it.message.orUnknown()}" }
            refreshing.value = false
        }
    }

    fun showAddFeedDialog() {
        errorMessage.value = null
        dialog.value = FeedsDialogState(showAddFeed = true)
    }

    fun dismissAddFeedDialog() {
        errorMessage.value = null
        dialog.updateCopy {
            copy(showAddFeed = false, showConfirmFeed = false, feedPreview = null, isSaving = false)
        }
    }

    fun onFeedUrlChanged(value: String) {
        dialog.updateCopy { copy(feedUrlInput = value) }
    }

    fun openTagEditor(feed: Feed) {
        dialog.updateCopy {
            copy(
                tagEditorFeedId = feed.id,
                tagEditorFeedTitle = feed.title,
                selectedTagIds = feed.tags.map { it.id }.toSet(),
                newTagNameInput = "",
            )
        }
    }

    fun closeTagEditor() {
        dialog.updateCopy { copy(tagEditorFeedId = null, selectedTagIds = emptySet(), isSaving = false) }
    }

    fun onNewTagChanged(value: String) {
        dialog.updateCopy { copy(newTagNameInput = value) }
    }

    fun toggleTagSelection(tagId: String) {
        dialog.updateCopy {
            copy(
                selectedTagIds = if (tagId in selectedTagIds) {
                    selectedTagIds - tagId
                } else {
                    selectedTagIds + tagId
                },
            )
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun persistSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            runCatching { settingsRepository.updateSettings(transform) }
        }
    }

    private fun MutableStateFlow<FeedsDialogState>.updateCopy(
        transform: FeedsDialogState.() -> FeedsDialogState,
    ) {
        update { it.transform() }
    }

    private fun String?.orUnknown(): String = takeIf { !isNullOrBlank() } ?: "unknown error"

    private data class FeedData(
        val feeds: List<Feed>,
        val tags: List<Tag>,
        val counts: UnreadCounts,
    )

    private data class SortData(val feed: FeedSort, val tag: TagSort)

    private data class StatusData(val isRefreshing: Boolean, val error: String?)

    private data class FeedsDialogState(
        val showAddFeed: Boolean = false,
        val showConfirmFeed: Boolean = false,
        val feedUrlInput: String = "",
        val feedPreview: FeedPreview? = null,
        val tagEditorFeedId: String? = null,
        val tagEditorFeedTitle: String = "",
        val newTagNameInput: String = "",
        val selectedTagIds: Set<String> = emptySet(),
        val isSaving: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
