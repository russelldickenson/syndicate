package com.zigzag.rssreader.feature.feeds.presentation

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.rssreader.core.domain.model.ArticleScope
import com.zigzag.rssreader.core.domain.model.Feed
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.TagSort
import com.zigzag.rssreader.core.ui.components.EmptyState
import com.zigzag.rssreader.core.ui.components.ErrorMessage
import com.zigzag.rssreader.core.ui.components.FeedRow
import com.zigzag.rssreader.core.ui.components.LoadingIndicator
import com.zigzag.rssreader.core.ui.components.SectionHeader
import com.zigzag.rssreader.core.ui.components.SortSheet
import com.zigzag.rssreader.core.ui.components.TagRow

@Composable
fun FeedsRoute(
    onNavigateToArticles: (scope: ArticleScope) -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val viewModel: FeedsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
    var showTagSortSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feeds") },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.semantics { contentDescription = "Open navigation" },
                    ) { Text("☰") }
                },
                actions = {
                    IconButton(
                        onClick = { showFeedSortSheet = true },
                        modifier = Modifier.semantics { contentDescription = "Sort feeds" },
                    ) { Text("⇅") }
                    IconButton(
                        onClick = { showTagSortSheet = true },
                        modifier = Modifier.semantics { contentDescription = "Sort tags" },
                    ) { Text("↕") }
                    IconButton(
                        onClick = viewModel::refreshAll,
                        enabled = !state.isRefreshing,
                        modifier = Modifier.semantics { contentDescription = "Refresh feeds" },
                    ) { Text("↻") }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.semantics { contentDescription = "Settings" },
                    ) { Text("⚙") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddFeedDialog,
                modifier = Modifier.semantics { contentDescription = "Add feed" },
            ) { Text("+") }
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
                    title = "No feeds yet",
                    message = "Add a feed to start reading your favorite sites.",
                    actionLabel = "Add feed",
                    onAction = viewModel::showAddFeedDialog,
                )
            } else {
                FeedList(
                    state = state,
                    onTagClick = { tagId -> onNavigateToArticles(ArticleScope.ForTag(tagId)) },
                    onFeedClick = { feedId -> onNavigateToArticles(ArticleScope.ForFeed(feedId)) },
                    onEditTags = viewModel::openTagEditor,
                )
            }
        }
    }

    if (showFeedSortSheet) {
        SortSheet(
            title = "Sort feeds",
            options = FeedSort.entries,
            selected = state.sort,
            onSelect = viewModel::setFeedSort,
            label = { it.label },
            onDismiss = { showFeedSortSheet = false },
        )
    }

    if (showTagSortSheet) {
        SortSheet(
            title = "Sort tags",
            options = TagSort.entries,
            selected = state.tagSort,
            onSelect = viewModel::setTagSort,
            label = { it.label },
            onDismiss = { showTagSortSheet = false },
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

    state.tagEditorFeedId?.let { feedId ->
        TagEditorDialog(feedId = feedId, state = state, viewModel = viewModel)
    }
}

@Composable
private fun FeedList(
    state: FeedsUiState,
    onTagClick: (String) -> Unit,
    onFeedClick: (String) -> Unit,
    onEditTags: (Feed) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (state.tags.isNotEmpty()) {
            item(key = "tags_header") { SectionHeader("Tags") }
            items(items = state.tags, key = { it.id }) { tag ->
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    TagRow(
                        tag = tag.copy(
                            unreadCount = state.unreadCounts.perTag[tag.id] ?: 0,
                        ),
                        onClick = { onTagClick(tag.id) },
                    )
                }
            }
        }
        item(key = "feeds_header") { SectionHeader("Feeds") }
        items(items = state.feeds, key = { it.id }) { feed ->
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                FeedRow(
                    feed = feed.copy(
                        unreadCount = state.unreadCounts.perFeed[feed.id] ?: feed.unreadCount,
                    ),
                    onFeedClick = { onFeedClick(feed.id) },
                    onEditTags = { onEditTags(feed) },
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
        title = { Text("Add feed") },
        text = {
            Column {
                TextField(
                    value = state.feedUrlInput,
                    onValueChange = viewModel::onFeedUrlChanged,
                    label = { Text("Feed URL") },
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
            ) { Text(if (state.isSaving) "Checking…" else "Continue") }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissAddFeedDialog) { Text("Cancel") }
        },
    )
}

@Composable
private fun DuplicateFeedMessage(
    feedId: String,
    onViewFeed: (String) -> Unit,
) {
    val message = buildAnnotatedString {
        append("You’re already subscribed to that feed.\n")
        withLink(LinkAnnotation.Clickable(tag = "view_feed") { onViewFeed(feedId) }) {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append("View feed →")
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
        title = { Text("Add this feed?") },
        text = {
            Column {
                Text(
                    text = "Add \"${state.feedPreviewTitle}\" to your feeds?",
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
            ) { Text(if (state.isSaving) "Adding…" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = viewModel::backToFeedUrl) { Text("Back") }
        },
    )
}

@Composable
private fun TagEditorDialog(feedId: String, state: FeedsUiState, viewModel: FeedsViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::closeTagEditor,
        title = { Text("Tags for ${state.tagEditorFeedTitle}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                state.tags.forEach { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = tag.id in state.selectedTagIds,
                            onCheckedChange = { viewModel.toggleTagSelection(tag.id) },
                        )
                        Text(text = tag.name, modifier = Modifier.weight(1f))
                    }
                }
                if (state.tags.isEmpty()) {
                    Text("No tags yet. Create one below.")
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = state.newTagNameInput,
                    onValueChange = viewModel::onNewTagChanged,
                    label = { Text("New tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { viewModel.addTag(state.newTagNameInput) }) {
                    Text("Create tag")
                }
                Text(
                    text = "Selected: ${state.selectedTagIds.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.setFeedTags(feedId, state.selectedTagIds) },
                enabled = !state.isSaving,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeTagEditor) { Text("Cancel") }
        },
    )
}
