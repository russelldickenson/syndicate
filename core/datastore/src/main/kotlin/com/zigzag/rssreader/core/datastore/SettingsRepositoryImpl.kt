package com.zigzag.rssreader.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.zigzag.rssreader.core.domain.model.ArticleFont
import com.zigzag.rssreader.core.domain.model.ArticleFontSize
import com.zigzag.rssreader.core.domain.model.ArticleSort
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.FolderSort
import com.zigzag.rssreader.core.domain.model.ThemeMode
import com.zigzag.rssreader.core.domain.model.UserSettings
import com.zigzag.rssreader.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences.toUserSettings() }

    override suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val current = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .first()
            .toUserSettings()

        val updated = transform(current)

        dataStore.edit { preferences ->
            preferences[SettingsKeys.THEME_MODE] = updated.themeMode.ordinal
            preferences[SettingsKeys.ARTICLE_FONT] = updated.articleFont.name
            preferences[SettingsKeys.ARTICLE_FONT_SIZE] = updated.articleFontSize.name
            preferences[SettingsKeys.REFRESH_ON_STARTUP] = updated.refreshOnStartup
            preferences[SettingsKeys.ARTICLE_SORT] = updated.articleSort.name
            preferences[SettingsKeys.FEED_SORT] = updated.feedSort.name
            preferences[SettingsKeys.FOLDER_SORT] = updated.folderSort.name
        }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val defaults = UserSettings()

        val themeMode = this[SettingsKeys.THEME_MODE]
            ?.let { ordinal -> ThemeMode.entries.getOrNull(ordinal) }
            ?: defaults.themeMode

        val articleFont = when (this[SettingsKeys.ARTICLE_FONT]) {
            "ATKINSON_SANS", "ATKINSON_SERIF" -> ArticleFont.ATKINSON
            else -> this[SettingsKeys.ARTICLE_FONT]
                ?.let { name -> ArticleFont.entries.firstOrNull { it.name == name } }
                ?: defaults.articleFont
        }

        val articleSort = this[SettingsKeys.ARTICLE_SORT]
            ?.let { name -> ArticleSort.entries.firstOrNull { it.name == name } }
            ?: defaults.articleSort

        val feedSort = this[SettingsKeys.FEED_SORT]
            ?.let { name -> FeedSort.entries.firstOrNull { it.name == name } }
            ?: defaults.feedSort

        val articleFontSize = this[SettingsKeys.ARTICLE_FONT_SIZE]
            ?.let { name -> ArticleFontSize.entries.firstOrNull { it.name == name } }
            ?: defaults.articleFontSize

        val folderSort = this[SettingsKeys.FOLDER_SORT]
            ?.let { name -> FolderSort.entries.firstOrNull { it.name == name } }
            ?: defaults.folderSort

        return UserSettings(
            themeMode = themeMode,
            articleFont = articleFont,
            articleFontSize = articleFontSize,
            refreshOnStartup = this[SettingsKeys.REFRESH_ON_STARTUP] ?: defaults.refreshOnStartup,
            articleSort = articleSort,
            feedSort = feedSort,
            folderSort = folderSort,
        )
    }
}
