package com.zigzag.rssreader.feature.articles.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zigzag.rssreader.core.domain.model.ArticleScope
import com.zigzag.rssreader.core.domain.model.ArticleSort
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.FolderSort
import com.zigzag.rssreader.core.domain.model.UserSettings
import com.zigzag.rssreader.core.domain.repository.ArticleRepository
import com.zigzag.rssreader.core.domain.repository.FeedRepository
import com.zigzag.rssreader.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticlesViewModel @Inject constructor(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val settingsRepository: SettingsRepository,
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val scope = MutableStateFlow<ArticleScope>(ArticleScope.All)
    private val unreadOnly = MutableStateFlow(false)
    private val sort = MutableStateFlow(ArticleSort.NEWEST)
    private val loading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val scopeTitle = MutableStateFlow("All articles")
    private val confirmMarkAllRead = MutableStateFlow(false)

    private val query =
        combine(scope, unreadOnly, sort) { currentScope, onlyUnread, currentSort ->
            ArticleQuery(currentScope, onlyUnread, currentSort)
        }

    private val articles =
        query
            .flatMapLatest { (currentScope, onlyUnread, currentSort) ->
                articleRepository.observeArticles(currentScope, onlyUnread, currentSort)
            }
            .onEach { loading.value = false }

    val uiState: StateFlow<ArticlesUiState> =
        combine(
            articles,
            query,
            combine(loading, errorMessage) { isLoading, error -> ArticleStatus(isLoading, error) },
            scopeTitle,
            confirmMarkAllRead,
        ) { list, currentQuery, status, title, showConfirm ->
            ArticlesUiState(
                articles = list,
                scope = currentQuery.scope,
                scopeTitle = title,
                sort = currentQuery.sort,
                unreadOnly = currentQuery.unreadOnly,
                showConfirmMarkAllRead = showConfirm,
                isLoading = status.isLoading,
                error = status.error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ArticlesUiState(),
        )

    init {
        // Scope is supplied by the route via setScope(); the handle is kept for state restoration.
        savedStateHandle.get<String>(KEY_SCOPE)?.let { restored ->
            setScope(
                when (restored) {
                    "unread" -> ArticleScope.Unread
                    "folder" -> savedStateHandle.get<String>("folderId")
                        ?.let(ArticleScope::ForFolder) ?: ArticleScope.All
                    "feed" -> savedStateHandle.get<String>("feedId")
                        ?.let(ArticleScope::ForFeed) ?: ArticleScope.All
                    else -> ArticleScope.All
                },
            )
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                sort.value = settings.articleSort
            }
        }
    }

    fun setScope(newScope: ArticleScope) {
        if (scope.value == newScope && scopeTitle.value.isNotBlank()) return
        scope.value = newScope
        loading.value = true
        errorMessage.value = null
        scopeTitle.value = titleFor(newScope)
        viewModelScope.launch {
            scopeTitle.value = resolveTitle(newScope)
        }
    }

    fun setSort(newSort: ArticleSort) {
        sort.value = newSort
        loading.value = true
        viewModelScope.launch {
            runCatching {
                settingsRepository.updateSettings { settings: UserSettings ->
                    settings.copy(articleSort = newSort)
                }
            }
        }
    }

    fun toggleUnreadOnly() {
        unreadOnly.value = !unreadOnly.value
        loading.value = true
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { articleRepository.markAllRead(scope.value) }
                .onFailure { errorMessage.value = "Couldn't mark all as read: ${it.message.orUnknown()}" }
        }
    }

    fun showMarkAllReadConfirmation() {
        confirmMarkAllRead.value = true
    }

    fun dismissMarkAllReadConfirmation() {
        confirmMarkAllRead.value = false
    }

    fun confirmMarkAllRead() {
        confirmMarkAllRead.value = false
        markAllRead()
    }

    fun markRead(articleId: String) {
        viewModelScope.launch {
            runCatching { articleRepository.markRead(articleId) }
                .onFailure { errorMessage.value = "Couldn't mark as read: ${it.message.orUnknown()}" }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun titleFor(currentScope: ArticleScope): String =
        when (currentScope) {
            ArticleScope.All -> "All articles"
            ArticleScope.Unread -> "Unread"
            is ArticleScope.ForFeed -> "Feed"
            is ArticleScope.ForFolder -> "Folder"
        }

    private suspend fun resolveTitle(currentScope: ArticleScope): String =
        when (currentScope) {
            ArticleScope.All -> "All articles"
            ArticleScope.Unread -> "Unread"
            is ArticleScope.ForFeed ->
                feedRepository.observeFeeds(FeedSort.NAME)
                    .firstOrNull()
                    ?.firstOrNull { it.id == currentScope.feedId }
                    ?.title ?: "Feed"
            is ArticleScope.ForFolder ->
                feedRepository.observeFolders(FolderSort.NAME)
                    .firstOrNull()
                    ?.firstOrNull { it.id == currentScope.folderId }
                    ?.name ?: "Folder"
        }

    private fun String?.orUnknown(): String = takeIf { !isNullOrBlank() } ?: "unknown error"

    private data class ArticleQuery(
        val scope: ArticleScope,
        val unreadOnly: Boolean,
        val sort: ArticleSort,
    )

    private data class ArticleStatus(val isLoading: Boolean, val error: String?)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val KEY_SCOPE = "scope"
    }
}
