package com.zigzag.syndicate.feature.articles.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.syndicate.core.domain.model.ArticleScope
import com.zigzag.syndicate.core.domain.model.ArticleSort
import com.zigzag.syndicate.core.ui.components.ArticleRow
import com.zigzag.syndicate.core.ui.components.FaviconImage
import com.zigzag.syndicate.core.ui.components.faviconCandidates
import com.zigzag.syndicate.core.ui.components.EmptyState
import com.zigzag.syndicate.core.ui.components.ErrorMessage
import com.zigzag.syndicate.core.ui.components.LoadingIndicator
import com.zigzag.syndicate.core.ui.components.SortSheet
import com.zigzag.syndicate.feature.articles.R

@Composable
fun ArticlesRoute(
    scope: ArticleScope,
    onOpenArticles: (List<String>, Int) -> Unit,
    onNavigateToFeeds: () -> Unit,
    onAddFeed: () -> Unit,
    onOpenDrawer: () -> Unit,
    title: String? = null,
    faviconBaseUrl: String? = null,
) {
    val viewModel: ArticlesViewModel = hiltViewModel()
    LaunchedEffect(scope) { viewModel.setScope(scope) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ArticlesScreen(
        state = state,
        viewModel = viewModel,
        routeTitle = title,
        faviconBaseUrl = faviconBaseUrl,
        onOpenArticles = onOpenArticles,
        onNavigateToFeeds = onNavigateToFeeds,
        onAddFeed = onAddFeed,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
internal fun ArticlesScreen(
    state: ArticlesUiState,
    viewModel: ArticlesViewModel,
    routeTitle: String?,
    faviconBaseUrl: String?,
    onOpenArticles: (List<String>, Int) -> Unit,
    onNavigateToFeeds: () -> Unit,
    onAddFeed: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    var showSortSheet by remember { mutableStateOf(false) }
    val displayTitle = routeTitle ?: state.scopeTitle
    val faviconUrls = remember(faviconBaseUrl) { faviconBaseUrl?.let(::faviconCandidates) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.semantics { contentDescription = "Open navigation" },
                    ) { Icon(Icons.Filled.Menu, contentDescription = null) }
                },
                actions = {
                    IconToggleButton(
                        checked = state.unreadOnly,
                        onCheckedChange = { viewModel.toggleUnreadOnly() },
                        modifier = Modifier.semantics {
                            contentDescription = if (state.unreadOnly) "Show all articles" else "Show unread only"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MarkEmailUnread,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (state.unreadOnly) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(
                        onClick = viewModel::showMarkAllReadConfirmation,
                        modifier = Modifier.semantics { contentDescription = "Mark all as read" },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MarkEmailRead,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier.semantics { contentDescription = "Sort articles" },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (faviconUrls != null) {
                    FaviconImage(
                        primaryUrl = faviconUrls.first,
                        fallbackUrl = faviconUrls.second,
                        fallback = {},
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            state.error?.let { ErrorMessage(message = it, onRetry = viewModel::clearError) }

            if (state.isLoading && state.articles.isEmpty()) {
                LoadingIndicator(modifier = Modifier.padding(16.dp))
            }

            if (!state.isLoading && state.articles.isEmpty()) {
                EmptyState(
                    title = "Nothing here yet",
                    message = when {
                        state.scope is ArticleScope.Favourites && state.hasFeeds ->
                            "Tap the heart on an article to save it here."
                        !state.hasFeeds ->
                            "Add a feed to start reading your favorite sites."
                        state.unreadOnly ->
                            "You're all caught up. Switch to All to see read articles."
                        else -> "Articles from your feeds will appear here."
                    },
                    actionLabel = if (state.hasFeeds) "Browse feeds" else "Add feeds",
                    onAction = if (state.hasFeeds) onNavigateToFeeds else onAddFeed,
                )
            } else {
                val listState = rememberLazyListState()
                // Always open at the top of the list: a plain LazyColumn would restore the
                // previous scroll offset when the activity (or saved back stack) is restored,
                // which shows the most-recently-loaded articles at the bottom on startup.
                // Reset whenever the underlying article set changes (new scope, refresh), but
                // leave the user's scroll alone otherwise.
                LaunchedEffect(state.articles) {
                    if (state.articles.isNotEmpty()) listState.scrollToItem(0)
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = state.articles,
                            key = { _, article -> article.id },
                        ) { index, article ->
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                ArticleRow(
                                    article = article,
                                    onRead = { onOpenArticles(state.articles.map { it.id }, index) },
                                )
                            }
                        }
                        item(key = "articles_bottom_spacer") {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }

                    // A slim scrollbar thumb shown whenever the list overflows the screen.
                    val thumb by remember {
                        derivedStateOf {
                            val info = listState.layoutInfo
                            val first = info.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf null
                            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf null
                            val total = info.totalItemsCount
                            if (total <= last.index - first.index + 1) {
                                null
                            } else {
                                first.index.toFloat() / total to (last.index + 1).toFloat() / total
                            }
                        }
                    }
                    thumb?.let { range ->
                        val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        Canvas(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(4.dp)
                                .padding(vertical = 8.dp),
                        ) {
                            val (start, end) = range
                            drawRoundRect(
                                color = barColor,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, start * size.height),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width,
                                    ((end - start).coerceAtLeast(0.03f) * size.height),
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        SortSheet(
            title = "Sort articles",
            options = ArticleSort.entries,
            selected = state.sort,
            onSelect = viewModel::setSort,
            label = { it.label },
            onDismiss = { showSortSheet = false },
        )
    }

    if (state.showConfirmMarkAllRead) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMarkAllReadConfirmation,
            title = { Text("Mark all as read?") },
            text = { Text("This marks every article in this view as read. They stay in the list, but unread-only view will be empty afterwards.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmMarkAllRead) { Text("Mark all read") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissMarkAllReadConfirmation) { Text("Cancel") }
            },
        )
    }
}
