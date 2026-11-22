package com.zigzag.rssreader.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.zigzag.rssreader.core.domain.model.ArticleSort
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.TagSort
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
            preferences[SettingsKeys.REFRESH_ON_STARTUP] = updated.refreshOnStartup
            preferences[SettingsKeys.ARTICLE_SORT] = updated.articleSort.name
            preferences[SettingsKeys.FEED_SORT] = updated.feedSort.name
            preferences[SettingsKeys.TAG_SORT] = updated.tagSort.name
        }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val defaults = UserSettings()

        val themeMode = this[SettingsKeys.THEME_MODE]
            ?.let { ordinal -> ThemeMode.entries.getOrNull(ordinal) }
            ?: defaults.themeMode

        val articleSort = this[SettingsKeys.ARTICLE_SORT]
            ?.let { name -> ArticleSort.entries.firstOrNull { it.name == name } }
            ?: defaults.articleSort

        val feedSort = this[SettingsKeys.FEED_SORT]
            ?.let { name -> FeedSort.entries.firstOrNull { it.name == name } }
            ?: defaults.feedSort

        val tagSort = this[SettingsKeys.TAG_SORT]
            ?.let { name -> TagSort.entries.firstOrNull { it.name == name } }
            ?: defaults.tagSort

        return UserSettings(
            themeMode = themeMode,
            refreshOnStartup = this[SettingsKeys.REFRESH_ON_STARTUP] ?: defaults.refreshOnStartup,
            articleSort = articleSort,
            feedSort = feedSort,
            tagSort = tagSort,
        )
    }
}
