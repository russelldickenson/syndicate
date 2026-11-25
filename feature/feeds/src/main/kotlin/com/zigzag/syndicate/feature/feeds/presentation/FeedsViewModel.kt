package com.zigzag.syndicate.feature.feeds.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zigzag.syndicate.core.domain.model.Feed
import com.zigzag.syndicate.core.domain.model.FeedAlreadyExistsException
import com.zigzag.syndicate.core.domain.model.FeedPreview
import com.zigzag.syndicate.core.domain.model.FeedSort
import com.zigzag.syndicate.core.domain.model.Folder
import com.zigzag.syndicate.core.domain.model.FolderSort
import com.zigzag.syndicate.core.domain.model.UnreadCounts
import com.zigzag.syndicate.core.domain.model.UserSettings
import com.zigzag.syndicate.core.domain.repository.ArticleRepository
import com.zigzag.syndicate.core.domain.repository.FeedRepository
import com.zigzag.syndicate.core.domain.repository.SettingsRepository
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
    private val folderSort = MutableStateFlow(FolderSort.NAME)
    private val refreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val dialog = MutableStateFlow(FeedsDialogState())

    private val feedsFlow: Flow<List<Feed>> =
        feedSort.flatMapLatest { sort -> feedRepository.observeFeeds(sort) }
    private val foldersFlow: Flow<List<Folder>> =
        folderSort.flatMapLatest { sort -> feedRepository.observeFolders(sort) }
    private val unreadCountsFlow: Flow<UnreadCounts> = feedRepository.observeUnreadCounts()

    val uiState: StateFlow<FeedsUiState> =
        combine(
            combine(feedsFlow, foldersFlow, unreadCountsFlow) { feeds, folders, counts ->
                FeedData(feeds, folders, counts)
            },
            combine(feedSort, folderSort) { feed, folder -> SortData(feed, folder) },
            combine(refreshing, errorMessage) { isRefreshing, error -> StatusData(isRefreshing, error) },
            dialog,
        ) { data, sorts, status, dialogState ->
            FeedsUiState(
                feeds = data.feeds,
                folders = data.folders,
                unreadCounts = data.counts,
                sort = sorts.feed,
                folderSort = sorts.folder,
                isRefreshing = status.isRefreshing,
                error = status.error,
                showAddFeedDialog = dialogState.showAddFeed,
                feedUrlInput = dialogState.feedUrlInput,
                showConfirmFeedDialog = dialogState.showConfirmFeed,
                feedPreviewTitle = dialogState.feedPreview?.title.orEmpty(),
                feedPreviewUrl = dialogState.feedPreview?.url.orEmpty(),
                duplicateFeedId = dialogState.duplicateFeed?.id,
                folderEditorFeedId = dialogState.folderEditorFeedId,
                folderEditorFeedTitle = dialogState.folderEditorFeedTitle,
                newFolderNameInput = dialogState.newFolderNameInput,
                selectedFolderId = dialogState.selectedFolderId,
                editingFolderId = dialogState.editingFolderId,
                editingFolderName = dialogState.editingFolderName,
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
                folderSort.value = settings.folderSort
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
                onFailure = ::handleAddFailure,
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
                onFailure = ::handleAddFailure,
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
                duplicateFeed = null,
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

    fun setFeedFolder(feedId: String, folderId: String?) {
        dialog.updateCopy { copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { feedRepository.setFeedFolder(feedId, folderId) }
                .onSuccess { dialog.updateCopy { copy(folderEditorFeedId = null, isSaving = false) } }
                .onFailure {
                    errorMessage.value = "Couldn't update folder: ${it.message.orUnknown()}"
                    dialog.updateCopy { copy(isSaving = false) }
                }
        }
    }

    fun addFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            errorMessage.value = "Enter a folder name."
            return
        }
        viewModelScope.launch {
            feedRepository.addFolder(trimmed).fold(
                onSuccess = { folder ->
                    dialog.updateCopy {
                        copy(newFolderNameInput = "", selectedFolderId = folder.id)
                    }
                    errorMessage.value = null
                },
                onFailure = { errorMessage.value = "Couldn't add folder: ${it.message.orUnknown()}" },
            )
        }
    }

    fun renameFolder(folderId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            errorMessage.value = "Enter a folder name."
            return
        }
        viewModelScope.launch {
            runCatching { feedRepository.renameFolder(folderId, trimmed) }
                .onSuccess { dialog.updateCopy { copy(editingFolderId = null) } }
                .onFailure { errorMessage.value = "Couldn't rename folder: ${it.message.orUnknown()}" }
        }
    }

    fun removeFolder(folderId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.removeFolder(folderId) }
                .onSuccess { dialog.updateCopy { copy(editingFolderId = null) } }
                .onFailure { errorMessage.value = "Couldn't remove folder: ${it.message.orUnknown()}" }
        }
    }

    fun setFeedSort(sort: FeedSort) {
        feedSort.value = sort
        persistSettings { it.copy(feedSort = sort) }
    }

    fun setFolderSort(sort: FolderSort) {
        folderSort.value = sort
        persistSettings { it.copy(folderSort = sort) }
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
            copy(
                showAddFeed = false,
                showConfirmFeed = false,
                feedPreview = null,
                duplicateFeed = null,
                isSaving = false,
            )
        }
    }

    fun onFeedUrlChanged(value: String) {
        dialog.updateCopy { copy(feedUrlInput = value, duplicateFeed = null) }
    }

    fun openFolderEditor(feed: Feed) {
        dialog.updateCopy {
            copy(
                folderEditorFeedId = feed.id,
                folderEditorFeedTitle = feed.title,
                selectedFolderId = feed.folderId,
                newFolderNameInput = "",
            )
        }
    }

    fun closeFolderEditor() {
        dialog.updateCopy { copy(folderEditorFeedId = null, isSaving = false) }
    }

    fun onNewFolderChanged(value: String) {
        dialog.updateCopy { copy(newFolderNameInput = value) }
    }

    fun selectFolder(folderId: String?) {
        dialog.updateCopy { copy(selectedFolderId = folderId) }
    }

    fun openFolderNameEditor(folder: Folder) {
        dialog.updateCopy {
            copy(editingFolderId = folder.id, editingFolderName = folder.name)
        }
    }

    fun onEditingFolderNameChanged(value: String) {
        dialog.updateCopy { copy(editingFolderName = value) }
    }

    fun closeFolderNameEditor() {
        dialog.updateCopy { copy(editingFolderId = null) }
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun handleAddFailure(throwable: Throwable) {
        if (throwable is FeedAlreadyExistsException) {
            errorMessage.value = null
            dialog.updateCopy {
                copy(
                    duplicateFeed = DuplicateFeed(throwable.feedId),
                    isSaving = false,
                )
            }
        } else {
            errorMessage.value = "Couldn't add feed: ${throwable.message.orUnknown()}"
            dialog.updateCopy { copy(isSaving = false) }
        }
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
        val folders: List<Folder>,
        val counts: UnreadCounts,
    )

    private data class SortData(val feed: FeedSort, val folder: FolderSort)

    private data class StatusData(val isRefreshing: Boolean, val error: String?)

    private data class DuplicateFeed(val id: String)

    private data class FeedsDialogState(
        val showAddFeed: Boolean = false,
        val showConfirmFeed: Boolean = false,
        val feedUrlInput: String = "",
        val feedPreview: FeedPreview? = null,
        val duplicateFeed: DuplicateFeed? = null,
        val folderEditorFeedId: String? = null,
        val folderEditorFeedTitle: String = "",
        val newFolderNameInput: String = "",
        val selectedFolderId: String? = null,
        val editingFolderId: String? = null,
        val editingFolderName: String = "",
        val isSaving: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
