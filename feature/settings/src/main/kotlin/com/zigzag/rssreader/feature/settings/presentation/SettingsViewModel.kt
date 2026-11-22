package com.zigzag.rssreader.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zigzag.rssreader.core.domain.model.ArticleSort
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.TagSort
import com.zigzag.rssreader.core.domain.model.ThemeMode
import com.zigzag.rssreader.core.domain.model.UserSettings
import com.zigzag.rssreader.core.domain.repository.FeedRepository
import com.zigzag.rssreader.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    feedRepository: FeedRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> =
        settingsRepository.observeSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UserSettings(),
            )

    fun setThemeMode(themeMode: ThemeMode) {
        update { it.copy(themeMode = themeMode) }
    }

    fun setRefreshOnStartup(enabled: Boolean) {
        update { it.copy(refreshOnStartup = enabled) }
    }

    fun setArticleSort(sort: ArticleSort) {
        update { it.copy(articleSort = sort) }
    }

    fun setFeedSort(sort: FeedSort) {
        update { it.copy(feedSort = sort) }
    }

    fun setTagSort(sort: TagSort) {
        update { it.copy(tagSort = sort) }
    }

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            runCatching { settingsRepository.updateSettings(transform) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
