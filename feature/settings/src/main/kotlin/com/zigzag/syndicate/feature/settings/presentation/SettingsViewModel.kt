package com.zigzag.syndicate.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zigzag.syndicate.core.domain.model.ArticleFont
import com.zigzag.syndicate.core.domain.model.ArticleFontSize
import com.zigzag.syndicate.core.domain.model.ArticleSort
import com.zigzag.syndicate.core.domain.model.FeedSort
import com.zigzag.syndicate.core.domain.model.FolderSort
import com.zigzag.syndicate.core.domain.model.ThemeMode
import com.zigzag.syndicate.core.domain.model.UserSettings
import com.zigzag.syndicate.core.domain.repository.FeedRepository
import com.zigzag.syndicate.core.domain.repository.SettingsRepository
import com.zigzag.syndicate.core.ui.R as CoreUiR
import com.zigzag.syndicate.core.ui.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val feedRepository: FeedRepository,
    private val strings: StringProvider,
) : ViewModel() {

    val exportFailedMessage: String = strings.string(CoreUiR.string.core_ui_msg_export_failed)
    val readFailedMessage: String = strings.string(CoreUiR.string.core_ui_msg_read_failed)
    val importFailedMessage: String = strings.string(CoreUiR.string.core_ui_msg_import_failed)

    fun exportedMessage(written: Boolean): String = strings.string(
        if (written) CoreUiR.string.core_ui_msg_feeds_exported else CoreUiR.string.core_ui_msg_write_failed,
    )

    fun importedMessage(added: Int, skipped: Int, failed: Int): String = if (failed > 0) {
        strings.string(CoreUiR.string.core_ui_msg_imported_with_failures, added, skipped, failed)
    } else {
        strings.string(CoreUiR.string.core_ui_msg_imported, added, skipped)
    }

    val settings: StateFlow<UserSettings> =
        settingsRepository.observeSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UserSettings(),
            )

    fun setArticleFontSize(size: ArticleFontSize) {
        update { it.copy(articleFontSize = size) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        update { it.copy(themeMode = themeMode) }
    }

    fun setArticleFont(font: ArticleFont) {
        update { it.copy(articleFont = font) }
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

    fun setFolderSort(sort: FolderSort) {
        update { it.copy(folderSort = sort) }
    }

    suspend fun exportSubscriptions(): Result<String> = feedRepository.exportSubscriptions()

    suspend fun importSubscriptions(opml: String): Result<com.zigzag.syndicate.core.domain.model.FeedImportSummary> =
        feedRepository.importSubscriptions(opml)

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            runCatching { settingsRepository.updateSettings(transform) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
