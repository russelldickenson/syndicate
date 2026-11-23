package com.zigzag.rssreader.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.rssreader.core.domain.model.ArticleSort
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.TagSort
import com.zigzag.rssreader.core.domain.model.ThemeMode
import com.zigzag.rssreader.core.domain.model.UserSettings
import com.zigzag.rssreader.core.ui.components.SectionHeader
import com.zigzag.rssreader.core.ui.components.SortSheet

@Composable
fun SettingsRoute(onOpenDrawer: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        viewModel = viewModel,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
internal fun SettingsScreen(
    settings: UserSettings,
    viewModel: SettingsViewModel,
    onOpenDrawer: () -> Unit,
) {
    var sortSheet by remember { mutableStateOf<SortKind?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RSS Reeder") },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.semantics { contentDescription = "Open navigation" },
                    ) { Text("☰") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item(key = "theme_header") { SectionHeader("Theme") }
            ThemeMode.entries.forEach { mode ->
                item(key = "theme_${mode.name}") {
                    PreferenceRow(
                        label = mode.label,
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                }
            }

            item(key = "sync_header") { SectionHeader("Sync") }
            item(key = "refresh_startup") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Refresh on startup",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Fetch new articles when the app opens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.refreshOnStartup,
                        onCheckedChange = viewModel::setRefreshOnStartup,
                    )
                }
            }

            item(key = "sort_header") { SectionHeader("Sorting") }
            item(key = "sort_articles") {
                PreferenceRow(
                    label = "Articles",
                    value = settings.articleSort.label,
                    onClick = { sortSheet = SortKind.ARTICLES },
                )
            }
            item(key = "sort_feeds") {
                PreferenceRow(
                    label = "Feeds",
                    value = settings.feedSort.label,
                    onClick = { sortSheet = SortKind.FEEDS },
                )
            }
            item(key = "sort_tags") {
                PreferenceRow(
                    label = "Tags",
                    value = settings.tagSort.label,
                    onClick = { sortSheet = SortKind.TAGS },
                )
            }
        }
    }

    when (sortSheet) {
        SortKind.ARTICLES -> SortSheet(
            title = "Sort articles",
            options = ArticleSort.entries,
            selected = settings.articleSort,
            onSelect = viewModel::setArticleSort,
            label = { it.label },
            onDismiss = { sortSheet = null },
        )
        SortKind.FEEDS -> SortSheet(
            title = "Sort feeds",
            options = FeedSort.entries,
            selected = settings.feedSort,
            onSelect = viewModel::setFeedSort,
            label = { it.label },
            onDismiss = { sortSheet = null },
        )
        SortKind.TAGS -> SortSheet(
            title = "Sort tags",
            options = TagSort.entries,
            selected = settings.tagSort,
            onSelect = viewModel::setTagSort,
            label = { it.label },
            onDismiss = { sortSheet = null },
        )
        null -> Unit
    }
}

@Composable
private fun PreferenceRow(
    label: String,
    value: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Text(text = "✓", color = MaterialTheme.colorScheme.primary)
        }
    }
}

private enum class SortKind { ARTICLES, FEEDS, TAGS }
