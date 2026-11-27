package com.zigzag.syndicate.feature.settings.presentation

import com.zigzag.syndicate.core.ui.components.contentDescriptionRes
import com.zigzag.syndicate.core.ui.util.labelRes
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
                        viewModel.exportedMessage(written),
                    )
                },
                onFailure = { snackbarHostState.showSnackbar(viewModel.exportFailedMessage) },
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
                snackbarHostState.showSnackbar(viewModel.readFailedMessage)
                return@launch
            }
            viewModel.importSubscriptions(opml).fold(
                onSuccess = { result ->
                    snackbarHostState.showSnackbar(
                        viewModel.importedMessage(
                            result.added,
                            result.skipped,
                            result.failed,
                        ),
                    )
                },
                onFailure = { snackbarHostState.showSnackbar(viewModel.importFailedMessage) },
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreUiR.string.core_ui_app_name)) },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.contentDescriptionRes(CoreUiR.string.core_ui_cd_open_navigation),
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
                                text = stringResource(mode.labelRes()),
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
                    label = stringResource(CoreUiR.string.core_ui_label_export_feeds),
                    value = stringResource(CoreUiR.string.core_ui_label_opml),
                    onClick = { exportLauncher.launch("syndicate-feeds.opml") },
                )
            }
            item(key = "import_feeds") {
                PreferenceRow(
                    label = stringResource(CoreUiR.string.core_ui_label_import_feeds),
                    value = stringResource(CoreUiR.string.core_ui_label_opml),
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
                            text = stringResource(CoreUiR.string.core_ui_label_refresh_on_startup),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(CoreUiR.string.core_ui_label_refresh_on_startup_summary),
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

            item(key = "sort_header") { SectionHeader(stringResource(CoreUiR.string.core_ui_label_sorting)) }
            item(key = "sort_articles") {
                PreferenceRow(
                    label = stringResource(CoreUiR.string.core_ui_label_articles),
                    value = stringResource(settings.articleSort.labelRes()),
                    onClick = { sortSheet = SortKind.ARTICLES },
                )
            }
            item(key = "sort_feeds") {
                PreferenceRow(
                    label = stringResource(CoreUiR.string.core_ui_title_feeds),
                    value = stringResource(settings.feedSort.labelRes()),
                    onClick = { sortSheet = SortKind.FEEDS },
                )
            }
            item(key = "sort_folders") {
                PreferenceRow(
                    label = stringResource(CoreUiR.string.core_ui_label_folders),
                    value = stringResource(settings.folderSort.labelRes()),
                    onClick = { sortSheet = SortKind.FOLDERS },
                )
            }
        }
    }

    when (sortSheet) {
        SortKind.ARTICLES -> SortSheet(
            title = stringResource(CoreUiR.string.core_ui_label_sort_articles),
            options = ArticleSort.entries,
            selected = settings.articleSort,
            onSelect = viewModel::setArticleSort,
            label = { stringResource(it.labelRes()) },
            onDismiss = { sortSheet = null },
        )
        SortKind.FEEDS -> SortSheet(
            title = stringResource(CoreUiR.string.core_ui_label_sort_feeds),
            options = FeedSort.entries,
            selected = settings.feedSort,
            onSelect = viewModel::setFeedSort,
            label = { stringResource(it.labelRes()) },
            onDismiss = { sortSheet = null },
        )
        SortKind.FOLDERS -> SortSheet(
            title = stringResource(CoreUiR.string.core_ui_label_sort_folders),
            options = FolderSort.entries,
            selected = settings.folderSort,
            onSelect = viewModel::setFolderSort,
            label = { stringResource(it.labelRes()) },
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
    val selectedLabel = if (selected) stringResource(CoreUiR.string.core_ui_state_selected) else null
    val preferenceAction = onClick
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = listOfNotNull(label, value).joinToString(", ")
                            if (selectedLabel != null) stateDescription = selectedLabel
                            onClick {
                                preferenceAction?.invoke()
                                true
                            }
                        }
                } else {
                    Modifier
                },
            )
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
            Text(text = stringResource(CoreUiR.string.core_ui_checkmark), color = MaterialTheme.colorScheme.primary)
        }
    }
}

private enum class SortKind { ARTICLES, FEEDS, FOLDERS }
