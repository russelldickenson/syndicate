package com.zigzag.rssreader.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.zigzag.rssreader.feature.articles.presentation.ArticlesRoute
import com.zigzag.rssreader.feature.feeds.presentation.FeedsRoute
import com.zigzag.rssreader.feature.reader.presentation.ReaderRoute
import com.zigzag.rssreader.feature.settings.presentation.SettingsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    data class Reader(val articleIds: List<String>, val initialIndex: Int = 0) : AppDestination
}

@HiltViewModel
class DrawerViewModel @Inject constructor(
    feedRepository: FeedRepository,
) : ViewModel() {

    val drawerState: StateFlow<DrawerViewModelState> =
        combine(
            feedRepository.observeTags(TagSort.NAME),
            feedRepository.observeFeeds(FeedSort.NAME),
            feedRepository.observeUnreadCounts(),
        ) { tags, feeds, counts -> DrawerViewModelState(tags, counts, feeds) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DrawerViewModelState(emptyList(), UnreadCounts(), emptyList()),
            )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class DrawerViewModelState(
    val tags: List<Tag>,
    val unreadCounts: UnreadCounts,
    val feeds: List<Feed> = emptyList(),
)

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(AppDestination.ArticlesAll)
    val drawerViewModel: DrawerViewModel = hiltViewModel()
    val appDrawerState by drawerViewModel.drawerState.collectAsStateWithLifecycle()

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

    fun openArticles(articleIds: List<String>, index: Int) {
        backStack.add(AppDestination.Reader(articleIds, index))
    }

    fun openScope(scope: ArticleScope) {
        when (scope) {
            is ArticleScope.ForTag -> {
                val title = appDrawerState.tags.firstOrNull { it.id == scope.tagId }?.name ?: scope.tagId
                backStack.add(AppDestination.TagArticles(scope.tagId, title))
            }
            is ArticleScope.ForFeed -> {
                val title = appDrawerState.feeds.firstOrNull { it.id == scope.feedId }?.title ?: scope.feedId
                backStack.add(AppDestination.FeedArticles(scope.feedId, title))
            }
            ArticleScope.Unread -> backStack.add(AppDestination.ArticlesUnread)
            ArticleScope.All -> backStack.add(AppDestination.ArticlesAll)
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val currentDestination = backStack.lastOrNull()
    val openDrawer: () -> Unit = { coroutineScope.launch { drawerState.open() } }

    val entries = entryProvider<NavKey> {
        entry<AppDestination.ArticlesAll> {
            ArticlesRoute(
                scope = ArticleScope.All,
                title = "All articles",
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds) },
                onOpenDrawer = openDrawer,
            )
        }
        entry<AppDestination.ArticlesUnread> {
            ArticlesRoute(
                scope = ArticleScope.Unread,
                title = "Unread",
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds) },
                onOpenDrawer = openDrawer,
            )
        }
        entry<AppDestination.TagArticles> { key ->
            ArticlesRoute(
                scope = ArticleScope.ForTag(key.tagId),
                title = key.title,
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds) },
                onOpenDrawer = openDrawer,
            )
        }
        entry<AppDestination.FeedArticles> { key ->
            ArticlesRoute(
                scope = ArticleScope.ForFeed(key.feedId),
                title = key.title,
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds) },
                onOpenDrawer = openDrawer,
            )
        }
        entry<AppDestination.Feeds> {
            FeedsRoute(
                onNavigateToArticles = ::openScope,
                onNavigateToSettings = { navigateTop(AppDestination.Settings) },
                onOpenDrawer = openDrawer,
            )
        }
        entry<AppDestination.Reader> { key ->
            ReaderRoute(
                articleIds = key.articleIds,
                initialIndex = key.initialIndex,
                onNavigateBack = ::navigateBack,
            )
        }
        entry<AppDestination.Settings> {
            SettingsRoute(
                onOpenDrawer = openDrawer,
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

    // The reader screen owns a per-article action bar, so no drawer is shown while it is on top.
    // The drawer itself stays inert there: there is no hamburger and edge-swipe cannot open a
    // ModalNavigationDrawer over the reader's WebView because the WebView consumes those drags.
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentDestination !is AppDestination.Reader,
        drawerContent = {
            AppDrawer(
                state = appDrawerState,
                currentDestination = currentDestination as? AppDestination,
                onNavigate = { destination ->
                    coroutineScope.launch { drawerState.close() }
                    navigateTop(destination)
                },
            )
        },
    ) {
        navContent()
    }
}
