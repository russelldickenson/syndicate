package com.zigzag.syndicate.core.ui.util

import androidx.annotation.StringRes
import com.zigzag.syndicate.core.domain.model.ArticleFont
import com.zigzag.syndicate.core.domain.model.ArticleFontSize
import com.zigzag.syndicate.core.domain.model.ArticleSort
import com.zigzag.syndicate.core.domain.model.FeedSort
import com.zigzag.syndicate.core.domain.model.FolderSort
import com.zigzag.syndicate.core.domain.model.ThemeMode
import com.zigzag.syndicate.core.ui.R

@StringRes
fun ArticleSort.labelRes(): Int = when (this) {
    ArticleSort.NEWEST -> R.string.core_ui_sort_newest
    ArticleSort.OLDEST -> R.string.core_ui_sort_oldest
    ArticleSort.TITLE -> R.string.core_ui_sort_title
    ArticleSort.FEED -> R.string.core_ui_sort_feed_name
}

@StringRes
fun FeedSort.labelRes(): Int = when (this) {
    FeedSort.NAME -> R.string.core_ui_sort_name
    FeedSort.UNREAD -> R.string.core_ui_sort_unread_count
    FeedSort.LAST_UPDATED -> R.string.core_ui_sort_last_updated
}

@StringRes
fun FolderSort.labelRes(): Int = when (this) {
    FolderSort.NAME -> R.string.core_ui_sort_name
    FolderSort.UNREAD -> R.string.core_ui_sort_unread_count
}

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.core_ui_theme_system
    ThemeMode.LIGHT -> R.string.core_ui_theme_light
    ThemeMode.DARK -> R.string.core_ui_theme_dark
}

@StringRes
fun ArticleFont.labelRes(): Int = when (this) {
    ArticleFont.SANS_SERIF -> R.string.core_ui_font_sans
    ArticleFont.SERIF -> R.string.core_ui_font_serif
    ArticleFont.INTER -> R.string.core_ui_font_inter
    ArticleFont.ATKINSON -> R.string.core_ui_font_atkinson
}

@StringRes
fun ArticleFontSize.labelRes(): Int = when (this) {
    ArticleFontSize.SMALL -> R.string.core_ui_font_size_small
    ArticleFontSize.MEDIUM -> R.string.core_ui_font_size_medium
    ArticleFontSize.LARGE -> R.string.core_ui_font_size_large
}
