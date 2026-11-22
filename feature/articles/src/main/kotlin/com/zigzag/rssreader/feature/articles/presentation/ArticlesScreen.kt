package com.zigzag.rssreader.feature.articles.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.rssreader.core.domain.model.ArticleScope
import com.zigzag.rssreader.core.domain.model.ArticleSort
import com.zigzag.rssreader.core.ui.components.ArticleRow
import com.zigzag.rssreader.core.ui.components.EmptyState
import com.zigzag.rssreader.core.ui.components.ErrorMessage
import com.zigzag.rssreader.core.ui.components.LoadingIndicator
import com.zigzag.rssreader.core.ui.components.SortSheet

@Composable
fun ArticlesRoute(
    scope: ArticleScope,
    onOpenArticle: (String) -> Unit,
    onNavigateToFeeds: () -> Unit,
    title: String? = null,
) {
    val viewModel: ArticlesViewModel = hiltViewModel()
    LaunchedEffect(scope) { viewModel.setScope(scope) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ArticlesScreen(
        state = state,
        viewModel = viewModel,
        routeTitle = title,
        onOpenArticle = onOpenArticle,
        onNavigateToFeeds = onNavigateToFeeds,
    )
}

@Composable
internal fun ArticlesScreen(
    state: ArticlesUiState,
    viewModel: ArticlesViewModel,
    routeTitle: String?,
    onOpenArticle: (String) -> Unit,
    onNavigateToFeeds: () -> Unit,
) {
    var showSortSheet by remember { mutableStateOf(false) }
    val displayTitle = routeTitle ?: state.scopeTitle

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayTitle) },
                actions = {
                    TextButton(
                        onClick = viewModel::toggleUnreadOnly,
                        modifier = Modifier.semantics {
                            contentDescription = if (state.unreadOnly) "Show all articles" else "Show unread only"
                        },
                    ) { Text(if (state.unreadOnly) "All" else "Unread") }
                    IconButton(
                        onClick = viewModel::markAllRead,
                        modifier = Modifier.semantics { contentDescription = "Mark all as read" },
                    ) { Text("✓") }
                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier.semantics { contentDescription = "Sort articles" },
                    ) { Text("⇅") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            state.error?.let { ErrorMessage(message = it, onRetry = viewModel::clearError) }

            if (state.isLoading && state.articles.isEmpty()) {
                LoadingIndicator(modifier = Modifier.padding(16.dp))
            }

            if (!state.isLoading && state.articles.isEmpty()) {
                EmptyState(
                    title = "Nothing here yet",
                    message = if (state.unreadOnly) {
                        "You're all caught up. Switch to All to see read articles."
                    } else {
                        "Articles from your feeds will appear here."
                    },
                    actionLabel = "Browse feeds",
                    onAction = onNavigateToFeeds,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.articles, key = { it.id }) { article ->
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            ArticleRow(
                                article = article,
                                onRead = { onOpenArticle(article.id) },
                            )
                        }
                    }
                    item(key = "articles_bottom_spacer") {
                        Spacer(modifier = Modifier.height(72.dp))
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
}
