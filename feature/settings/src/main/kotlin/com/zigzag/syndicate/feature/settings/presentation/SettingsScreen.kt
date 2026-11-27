package com.zigzag.syndicate.feature.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.syndicate.core.domain.model.ArticleFont
import com.zigzag.syndicate.core.domain.model.ArticleSort
import com.zigzag.syndicate.core.domain.model.FeedSort
import com.zigzag.syndicate.core.domain.model.FolderSort
import com.zigzag.syndicate.core.domain.model.ThemeMode
import com.zigzag.syndicate.core.domain.model.UserSettings
import com.zigzag.syndicate.core.ui.components.ArticleFontPreview
import com.zigzag.syndicate.core.ui.components.ArticleFontChipSelector
import com.zigzag.syndicate.core.ui.components.ArticleFontSizeSelector
import com.zigzag.syndicate.core.ui.components.SectionHeader
import com.zigzag.syndicate.core.ui.components.SortSheet
import com.zigzag.syndicate.core.ui.R as CoreUiR
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/xml"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.exportSubscriptions().fold(
                onSuccess = { opml ->
                    val written = runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(opml.toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                    snackbarHostState.showSnackbar(
                        if (written) "Feeds exported" else "Could not write the export file",
                    )
                },
                onFailure = { snackbarHostState.showSnackbar("Could not export feeds") },
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val opml = runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
            }.getOrNull()
            if (opml == null) {
                snackbarHostState.showSnackbar("Could not read the selected file")
                return@launch
            }
            viewModel.importSubscriptions(opml).fold(
                onSuccess = { result ->
                    snackbarHostState.showSnackbar(
                        "Imported ${result.added} feed(s), ${result.skipped} duplicate(s) skipped" +
                            if (result.failed > 0) ", ${result.failed} failed" else "",
                    )
                },
                onFailure = { snackbarHostState.showSnackbar("Could not import feeds") },
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Syndicate") },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.semantics { contentDescription = "Open navigation" },
                    ) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.core_ui_ic_menu),
                            contentDescription = null,
                        )
                    }
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
            item(key = "theme_selector") {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = mode.ordinal,
                                count = ThemeMode.entries.size,
                            ),
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            item(key = "reading_header") { SectionHeader("Reading") }
            item(key = "article_font") { SectionHeader("Font") }
            item(key = "article_font_selector") {
                ArticleFontChipSelector(
                    selected = settings.articleFont,
                    onSelect = viewModel::setArticleFont,
                )
            }
            item(key = "article_font_size") { SectionHeader("Text size") }
            item(key = "article_font_size_selector") {
                ArticleFontSizeSelector(
                    selected = settings.articleFontSize,
                    onSelect = viewModel::setArticleFontSize,
                )
            }
            item(key = "article_font_preview") {
                ArticleFontPreview(
                    font = settings.articleFont,
                    size = settings.articleFontSize,
                )
            }

            item(key = "subscriptions_header") { SectionHeader("Subscriptions") }
            item(key = "export_feeds") {
                PreferenceRow(
                    label = "Export feeds",
                    value = "OPML file",
                    onClick = { exportLauncher.launch("syndicate-feeds.opml") },
                )
            }
            item(key = "import_feeds") {
                PreferenceRow(
                    label = "Import feeds",
                    value = "OPML file",
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                )
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
            item(key = "sort_folders") {
                PreferenceRow(
                    label = "Folders",
                    value = settings.folderSort.label,
                    onClick = { sortSheet = SortKind.FOLDERS },
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
        SortKind.FOLDERS -> SortSheet(
            title = "Sort folders",
            options = FolderSort.entries,
            selected = settings.folderSort,
            onSelect = viewModel::setFolderSort,
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

private enum class SortKind { ARTICLES, FEEDS, FOLDERS }
