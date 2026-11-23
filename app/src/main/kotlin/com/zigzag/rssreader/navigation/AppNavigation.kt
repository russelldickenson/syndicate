package com.zigzag.rssreader.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.zigzag.rssreader.core.domain.model.ArticleScope
import com.zigzag.rssreader.core.domain.model.Feed
import com.zigzag.rssreader.core.domain.model.FeedSort
import com.zigzag.rssreader.core.domain.model.Tag
import com.zigzag.rssreader.core.domain.model.TagSort
import com.zigzag.rssreader.core.domain.model.UnreadCounts
import com.zigzag.rssreader.core.domain.repository.FeedRepository
import com.zigzag.rssreader.core.ui.components.UnreadBadge
import com.zigzag.rssreader.feature.articles.presentation.ArticlesRoute
import com.zigzag.rssreader.feature.feeds.presentation.FeedsRoute
import com.zigzag.rssreader.feature.reader.presentation.ReaderRoute
import com.zigzag.rssreader.feature.settings.presentation.SettingsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object ArticlesAll : AppDestination

    @Serializable
    data object ArticlesUnread : AppDestination

    @Serializable
    data object Feeds : AppDestination

    @Serializable
    data object Settings : AppDestination

    @Serializable
    data class TagArticles(val tagId: String, val title: String) : AppDestination

    @Serializable
    data class FeedArticles(val feedId: String, val title: String) : AppDestination

    @Serializable
    data class Reader(val articleId: String) : AppDestination
}

@HiltViewModel
class DrawerViewModel @Inject constructor(
    feedRepository: FeedRepository,
) : ViewModel() {

    val drawerState: StateFlow<DrawerState> =
        combine(
            feedRepository.observeTags(TagSort.NAME),
            feedRepository.observeFeeds(FeedSort.NAME),
            feedRepository.observeUnreadCounts(),
        ) { tags, feeds, counts -> DrawerState(tags, counts, feeds) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DrawerState(emptyList(), UnreadCounts(), emptyList()),
            )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class DrawerState(
    val tags: List<Tag>,
    val unreadCounts: UnreadCounts,
    val feeds: List<Feed> = emptyList(),
)

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(AppDestination.ArticlesAll)
    val drawerViewModel: DrawerViewModel = hiltViewModel()
    val drawerState by drawerViewModel.drawerState.collectAsStateWithLifecycle()

    // A restored or back-popped stack must never be empty: NavDisplay requires at least one entry.
    if (backStack.isEmpty()) {
        LaunchedEffect(Unit) { backStack.add(AppDestination.ArticlesAll) }
        return
    }

    fun navigateTop(destination: AppDestination) {
        backStack.clear()
        backStack.add(destination)
    }

    fun navigateBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun openArticle(articleId: String) {
        backStack.add(AppDestination.Reader(articleId))
    }

    fun openScope(scope: ArticleScope) {
        when (scope) {
            is ArticleScope.ForTag -> {
                val title = drawerState.tags.firstOrNull { it.id == scope.tagId }?.name ?: scope.tagId
                backStack.add(AppDestination.TagArticles(scope.tagId, title))
            }
            is ArticleScope.ForFeed -> {
                val title = drawerState.feeds.firstOrNull { it.id == scope.feedId }?.title ?: scope.feedId
                backStack.add(AppDestination.FeedArticles(scope.feedId, title))
            }
            ArticleScope.Unread -> backStack.add(AppDestination.ArticlesUnread)
            ArticleScope.All -> backStack.add(AppDestination.ArticlesAll)
        }
    }

    val entries = entryProvider<NavKey> {
        entry<AppDestination.ArticlesAll> {
            ArticlesRoute(
                scope = ArticleScope.All,
                title = "All articles",
                onOpenArticle = ::openArticle,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds) },
            )
        }
        entry<AppDestination.ArticlesUnread> {
            ArticlesRoute(
                scope = ArticleScope.Unread,
                title = "Unread",
                onOpenArticle = ::openArticle,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds) },
            )
        }
        entry<AppDestination.TagArticles> { key ->
            ArticlesRoute(
                scope = ArticleScope.ForTag(key.tagId),
                title = key.title,
                onOpenArticle = ::openArticle,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds) },
            )
        }
        entry<AppDestination.FeedArticles> { key ->
            ArticlesRoute(
                scope = ArticleScope.ForFeed(key.feedId),
                title = key.title,
                onOpenArticle = ::openArticle,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds) },
            )
        }
        entry<AppDestination.Feeds> {
            FeedsRoute(
                onNavigateToArticles = ::openScope,
                onNavigateToSettings = { navigateTop(AppDestination.Settings) },
            )
        }
        entry<AppDestination.Reader> { key ->
            ReaderRoute(
                articleId = key.articleId,
                onNavigateBack = ::navigateBack,
            )
        }
        entry<AppDestination.Settings> {
            SettingsRoute(
                onNavigateBack = ::navigateBack,
            )
        }
    }

    val navContent: @Composable () -> Unit = {
        NavDisplay(
            backStack = backStack,
            onBack = ::navigateBack,
            entryProvider = entries,
        )
    }

    // The reader screen owns a per-article action bar, so the top-level selector is hidden.
    if (backStack.lastOrNull() is AppDestination.Reader) {
        navContent()
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                item(
                    icon = { Text("☰") },
                    label = { Text("All") },
                    selected = backStack.lastOrNull() == AppDestination.ArticlesAll,
                    onClick = { navigateTop(AppDestination.ArticlesAll) },
                )
                item(
                    icon = { Text("★") },
                    label = { Text("Unread") },
                    badge = {
                        val count = drawerState.unreadCounts.global
                        if (count > 0) UnreadBadge(count = count)
                    },
                    selected = backStack.lastOrNull() == AppDestination.ArticlesUnread,
                    onClick = { navigateTop(AppDestination.ArticlesUnread) },
                )
                item(
                    icon = { Text("⚑") },
                    label = { Text("Feeds") },
                    selected = backStack.lastOrNull() == AppDestination.Feeds,
                    onClick = { navigateTop(AppDestination.Feeds) },
                )
                drawerState.tags.forEach { tag ->
                    item(
                        icon = { Text("#") },
                        label = { Text(tag.name) },
                        badge = { UnreadBadge(count = drawerState.unreadCounts.perTag[tag.id] ?: 0) },
                        selected = backStack.lastOrNull() == AppDestination.TagArticles(tag.id, tag.name),
                        onClick = { navigateTop(AppDestination.TagArticles(tag.id, tag.name)) },
                    )
                }
                item(
                    icon = { Text("⚙") },
                    label = { Text("Settings") },
                    selected = backStack.lastOrNull() == AppDestination.Settings,
                    onClick = { navigateTop(AppDestination.Settings) },
                )
            },
        ) {
            navContent()
        }
    }
}
