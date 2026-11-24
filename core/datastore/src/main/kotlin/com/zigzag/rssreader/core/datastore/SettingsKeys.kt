package com.zigzag.rssreader.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val THEME_MODE = intPreferencesKey("theme_mode")
    val ARTICLE_FONT = stringPreferencesKey("article_font")
    val REFRESH_ON_STARTUP = booleanPreferencesKey("refresh_on_startup")
    val ARTICLE_SORT = stringPreferencesKey("article_sort")
    val FEED_SORT = stringPreferencesKey("feed_sort")
    val FOLDER_SORT = stringPreferencesKey("folder_sort")
}
