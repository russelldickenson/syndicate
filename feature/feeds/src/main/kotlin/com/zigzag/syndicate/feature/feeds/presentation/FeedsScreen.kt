package com.zigzag.syndicate.feature.feeds.presentation

import com.zigzag.syndicate.core.ui.components.contentDescriptionRes
import androidx.compose.ui.res.stringResource
import com.zigzag.syndicate.core.ui.util.labelRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.syndicate.core.domain.model.ArticleScope
import com.zigzag.syndicate.core.domain.model.Feed
import com.zigzag.syndicate.core.domain.model.FeedSort
import com.zigzag.syndicate.core.domain.model.FolderSort
import com.zigzag.syndicate.core.domain.model.Folder
import com.zigzag.syndicate.core.ui.components.EmptyState
import com.zigzag.syndicate.core.ui.components.ErrorMessage
import com.zigzag.syndicate.core.ui.components.FeedRow
import com.zigzag.syndicate.core.ui.components.LoadingIndicator
import com.zigzag.syndicate.core.ui.components.SectionHeader
import com.zigzag.syndicate.core.ui.components.SortSheet
import com.zigzag.syndicate.core.ui.components.FolderRow
import com.zigzag.syndicate.core.ui.R as CoreUiR

@Composable
fun FeedsRoute(
    onNavigateToArticles: (scope: ArticleScope) -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    openAddFeed: Boolean = false,
) {
    val viewModel: FeedsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(openAddFeed) {
        if (openAddFeed) viewModel.showAddFeedDialog()
    }

    FeedsScreen(
        state = state,
        viewModel = viewModel,
        onNavigateToArticles = onNavigateToArticles,
        onNavigateToSettings = onNavigateToSettings,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
internal fun FeedsScreen(
    state: FeedsUiState,
    viewModel: FeedsViewModel,
    onNavigateToArticles: (ArticleScope) -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    var showFeedSortSheet by remember { mutableStateOf(false) }
    var showFolderSortSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CoreUiR.string.core_ui_title_feeds)) },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.contentDescriptionRes(CoreUiR.string.core_ui_cd_open_navigation),
                    ) { Icon(painter = painterResource(CoreUiR.drawable.core_ui_ic_menu), contentDescription = null) }
                },
                actions = {
                    IconButton(
                        onClick = { showFeedSortSheet = true },
                        modifier = Modifier.contentDescriptionRes(CoreUiR.string.core_ui_cd_sort_feeds),
                    ) { Icon(painter = painterResource(CoreUiR.drawable.core_ui_ic_sort), contentDescription = null) }
                    IconButton(
                        onClick = { showFolderSortSheet = true },
                        modifier = Modifier.contentDescriptionRes(CoreUiR.string.core_ui_cd_sort_folders),
                    ) { Icon(painter = painterResource(CoreUiR.drawable.core_ui_ic_swap_vert), contentDescription = null) }
                    IconButton(
                        onClick = viewModel::refreshAll,
                        enabled = !state.isRefreshing,
                        modifier = Modifier.contentDescriptionRes(CoreUiR.string.core_ui_cd_refresh_feeds),
                    ) { Icon(painter = painterResource(CoreUiR.drawable.core_ui_ic_refresh), contentDescription = null) }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.contentDescriptionRes(CoreUiR.string.core_ui_label_settings),
                    ) { Icon(painter = painterResource(CoreUiR.drawable.core_ui_ic_settings), contentDescription = null) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddFeedDialog,
                modifier = Modifier.contentDescriptionRes(CoreUiR.string.core_ui_cd_add_feed),
            ) { Text(stringResource(CoreUiR.string.core_ui_add_symbol)) }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (state.isRefreshing) {
                LoadingIndicator(modifier = Modifier.padding(16.dp))
            }
            state.error?.let { message ->
                ErrorMessage(
                    message = message,
                    onRetry = {
                        viewModel.clearError()
                        viewModel.refreshAll()
                    },
                )
            }

            if (state.feeds.isEmpty()) {
                EmptyState(
                    title = stringResource(CoreUiR.string.core_ui_empty_no_feeds_title),
                    message = stringResource(CoreUiR.string.core_ui_empty_no_feeds),
                    actionLabel = stringResource(CoreUiR.string.core_ui_cd_add_feed),
                    onAction = viewModel::showAddFeedDialog,
                )
            } else {
                FeedList(
                    state = state,
                    onFolderClick = { folderId -> onNavigateToArticles(ArticleScope.ForFolder(folderId)) },
                    onFeedClick = { feedId -> onNavigateToArticles(ArticleScope.ForFeed(feedId)) },
                    onEditFolder = viewModel::openFolderEditor,
                    onEditFolderDetails = viewModel::openFolderNameEditor,
                )
            }
        }
    }

    if (showFeedSortSheet) {
        SortSheet(
            title = stringResource(CoreUiR.string.core_ui_label_sort_feeds),
            options = FeedSort.entries,
            selected = state.sort,
            onSelect = viewModel::setFeedSort,
            label = { stringResource(it.labelRes()) },
            onDismiss = { showFeedSortSheet = false },
        )
    }

    if (showFolderSortSheet) {
        SortSheet(
            title = stringResource(CoreUiR.string.core_ui_label_sort_folders),
            options = FolderSort.entries,
            selected = state.folderSort,
            onSelect = viewModel::setFolderSort,
            label = { stringResource(it.labelRes()) },
            onDismiss = { showFolderSortSheet = false },
        )
    }

    val viewFeed: (String) -> Unit = { feedId ->
        viewModel.dismissAddFeedDialog()
        onNavigateToArticles(ArticleScope.ForFeed(feedId))
    }

    if (state.showAddFeedDialog) {
        AddFeedDialog(state = state, viewModel = viewModel, onViewFeed = viewFeed)
    }

    if (state.showConfirmFeedDialog) {
        ConfirmFeedDialog(state = state, viewModel = viewModel, onViewFeed = viewFeed)
    }

    state.folderEditorFeedId?.let { feedId ->
        FolderEditorDialog(feedId = feedId, state = state, viewModel = viewModel)
    }

    state.editingFolderId?.let { folderId ->
        FolderNameEditorDialog(folderId = folderId, state = state, viewModel = viewModel)
    }
}

@Composable
private fun FeedList(
    state: FeedsUiState,
    onFolderClick: (String) -> Unit,
    onFeedClick: (String) -> Unit,
    onEditFolder: (Feed) -> Unit,
    onEditFolderDetails: (Folder) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (state.folders.isNotEmpty()) {
            item(key = "folders_header") { SectionHeader(stringResource(CoreUiR.string.core_ui_label_folders)) }
            items(items = state.folders, key = { it.id }) { folder ->
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    FolderRow(
                        folder = folder.copy(
                            unreadCount = state.unreadCounts.perFolder[folder.id] ?: 0,
                        ),
                        onClick = { onFolderClick(folder.id) },
                        onEdit = { onEditFolderDetails(folder) },
                    )
                }
            }
        }
        item(key = "feeds_header") { SectionHeader(stringResource(CoreUiR.string.core_ui_title_feeds)) }
        items(items = state.feeds, key = { it.id }) { feed ->
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                FeedRow(
                    feed = feed.copy(
                        unreadCount = state.unreadCounts.perFeed[feed.id] ?: feed.unreadCount,
                    ),
                    onFeedClick = { onFeedClick(feed.id) },
                    onEditFolder = { onEditFolder(feed) },
                )
            }
        }
    }
}

@Composable
private fun AddFeedDialog(
    state: FeedsUiState,
    viewModel: FeedsViewModel,
    onViewFeed: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = viewModel::dismissAddFeedDialog,
        title = { Text(stringResource(CoreUiR.string.core_ui_dialog_add_feed_title)) },
        text = {
            Column {
                TextField(
                    value = state.feedUrlInput,
                    onValueChange = viewModel::onFeedUrlChanged,
                    label = { Text(stringResource(CoreUiR.string.core_ui_label_feed_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.duplicateFeedId?.let { feedId ->
                    Spacer(Modifier.height(8.dp))
                    DuplicateFeedMessage(feedId = feedId, onViewFeed = onViewFeed)
                }
                if (state.duplicateFeedId == null && state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.previewFeed(state.feedUrlInput) },
                enabled = !state.isSaving,
            ) { Text(stringResource(if (state.isSaving) CoreUiR.string.core_ui_action_checking else CoreUiR.string.core_ui_action_continue)) }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissAddFeedDialog) { Text(stringResource(CoreUiR.string.core_ui_action_cancel)) }
        },
    )
}

@Composable
private fun DuplicateFeedMessage(
    feedId: String,
    onViewFeed: (String) -> Unit,
) {
    val message = buildAnnotatedString {
        append(stringResource(CoreUiR.string.core_ui_msg_duplicate_feed))
        append("\n")
        withLink(LinkAnnotation.Clickable(tag = "view_feed") { onViewFeed(feedId) }) {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(stringResource(CoreUiR.string.core_ui_action_view_feed))
            }
        }
    }
    Text(text = message, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ConfirmFeedDialog(
    state: FeedsUiState,
    viewModel: FeedsViewModel,
    onViewFeed: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = viewModel::backToFeedUrl,
        title = { Text(stringResource(CoreUiR.string.core_ui_dialog_add_feed_confirm_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(CoreUiR.string.core_ui_dialog_add_feed_message, state.feedPreviewTitle),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.feedPreviewUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.duplicateFeedId?.let { feedId ->
                    Spacer(Modifier.height(8.dp))
                    DuplicateFeedMessage(feedId = feedId, onViewFeed = onViewFeed)
                }
                if (state.duplicateFeedId == null && state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::confirmAddFeed,
                enabled = !state.isSaving,
            ) { Text(stringResource(if (state.isSaving) CoreUiR.string.core_ui_action_adding else CoreUiR.string.core_ui_action_add)) }
        },
        dismissButton = {
            TextButton(onClick = viewModel::backToFeedUrl) { Text(stringResource(CoreUiR.string.core_ui_action_back)) }
        },
    )
}

@Composable
private fun FolderEditorDialog(feedId: String, state: FeedsUiState, viewModel: FeedsViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::closeFolderEditor,
        title = { Text(stringResource(CoreUiR.string.core_ui_dialog_folder_for_title, state.folderEditorFeedTitle)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectFolder(null) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = state.selectedFolderId == null,
                        onClick = { viewModel.selectFolder(null) },
                    )
                    Text(stringResource(CoreUiR.string.core_ui_label_no_folder))
                }
                state.folders.forEach { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectFolder(folder.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.selectedFolderId == folder.id,
                            onClick = { viewModel.selectFolder(folder.id) },
                        )
                        Text(text = folder.name, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = state.newFolderNameInput,
                    onValueChange = viewModel::onNewFolderChanged,
                    label = { Text(stringResource(CoreUiR.string.core_ui_label_new_folder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { viewModel.addFolder(state.newFolderNameInput) }) {
                    Text(stringResource(CoreUiR.string.core_ui_action_create_folder))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.setFeedFolder(feedId, state.selectedFolderId) },
                enabled = !state.isSaving,
            ) { Text(stringResource(CoreUiR.string.core_ui_action_save)) }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeFolderEditor) { Text(stringResource(CoreUiR.string.core_ui_action_cancel)) }
        },
    )
}

@Composable
private fun FolderNameEditorDialog(
    folderId: String,
    state: FeedsUiState,
    viewModel: FeedsViewModel,
) {
    AlertDialog(
        onDismissRequest = viewModel::closeFolderNameEditor,
        title = { Text(stringResource(CoreUiR.string.core_ui_dialog_edit_folder_title)) },
        text = {
            Column {
                TextField(
                    value = state.editingFolderName,
                    onValueChange = viewModel::onEditingFolderNameChanged,
                    label = { Text(stringResource(CoreUiR.string.core_ui_label_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.removeFolder(folderId) }) {
                    Text(stringResource(CoreUiR.string.core_ui_action_delete_folder))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.renameFolder(folderId, state.editingFolderName) }) {
                Text(stringResource(CoreUiR.string.core_ui_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeFolderNameEditor) { Text(stringResource(CoreUiR.string.core_ui_action_cancel)) }
        },
    )
}
