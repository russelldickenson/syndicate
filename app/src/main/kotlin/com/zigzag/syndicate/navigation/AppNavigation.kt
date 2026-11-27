package com.zigzag.syndicate.navigation

import com.zigzag.syndicate.core.ui.R as CoreUiR
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
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
import androidx.window.core.layout.WindowSizeClass
import com.zigzag.syndicate.core.domain.model.ArticleScope
import com.zigzag.syndicate.core.domain.model.Feed
import com.zigzag.syndicate.core.domain.model.FeedSort
import com.zigzag.syndicate.core.domain.model.Folder
import com.zigzag.syndicate.core.domain.model.FolderSort
import com.zigzag.syndicate.core.domain.model.UnreadCounts
import com.zigzag.syndicate.core.domain.repository.FeedRepository
import com.zigzag.syndicate.feature.articles.presentation.ArticlesRoute
import com.zigzag.syndicate.feature.feeds.presentation.FeedsRoute
import com.zigzag.syndicate.feature.reader.presentation.ReaderRoute
import com.zigzag.syndicate.feature.settings.presentation.SettingsRoute
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
    data object ArticlesFavourites : AppDestination

    @Serializable
    data class Feeds(val openAddFeed: Boolean = false) : AppDestination

    @Serializable
    data object Settings : AppDestination

    @Serializable
    data class FolderArticles(val folderId: String, val title: String) : AppDestination

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
            feedRepository.observeFolders(FolderSort.NAME),
            feedRepository.observeFeeds(FeedSort.NAME),
            feedRepository.observeUnreadCounts(),
        ) { folders, feeds, counts -> DrawerViewModelState(folders, counts, feeds) }
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
    val folders: List<Folder>,
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
            is ArticleScope.ForFolder -> {
                val title = appDrawerState.folders.firstOrNull { it.id == scope.folderId }?.name
                    ?: scope.folderId
                backStack.add(AppDestination.FolderArticles(scope.folderId, title))
            }
            is ArticleScope.ForFeed -> {
                val title = appDrawerState.feeds.firstOrNull { it.id == scope.feedId }?.title ?: scope.feedId
                backStack.add(AppDestination.FeedArticles(scope.feedId, title))
            }
            ArticleScope.Unread -> backStack.add(AppDestination.ArticlesUnread)
            ArticleScope.Favourites -> backStack.add(AppDestination.ArticlesFavourites)
            ArticleScope.All -> backStack.add(AppDestination.ArticlesAll)
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val currentDestination = backStack.lastOrNull()
    val openDrawer: () -> Unit = { coroutineScope.launch { drawerState.open() } }
    val openAddFeed: () -> Unit = { navigateTop(AppDestination.Feeds(openAddFeed = true)) }

    val wideLayout = currentWindowAdaptiveInfo()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val entries = entryProvider<NavKey> {
        entry<AppDestination.ArticlesAll> {
            ArticlesRoute(
                scope = ArticleScope.All,
                title = stringResource(CoreUiR.string.core_ui_title_all_articles),
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds()) },
                onAddFeed = openAddFeed,
                onOpenDrawer = openDrawer,

                showNavigationIcon = !wideLayout,
            )
        }
        entry<AppDestination.ArticlesUnread> {
            ArticlesRoute(
                scope = ArticleScope.Unread,
                title = stringResource(CoreUiR.string.core_ui_title_unread),
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds()) },
                onAddFeed = openAddFeed,
                onOpenDrawer = openDrawer,

                showNavigationIcon = !wideLayout,
            )
        }
        entry<AppDestination.ArticlesFavourites> {
            ArticlesRoute(
                scope = ArticleScope.Favourites,
                title = stringResource(CoreUiR.string.core_ui_title_favourites),
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds()) },
                onAddFeed = openAddFeed,
                onOpenDrawer = openDrawer,

                showNavigationIcon = !wideLayout,
            )
        }
        entry<AppDestination.FolderArticles> { key ->
            ArticlesRoute(
                scope = ArticleScope.ForFolder(key.folderId),
                title = key.title,
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds()) },
                onAddFeed = openAddFeed,
                onOpenDrawer = openDrawer,

                showNavigationIcon = !wideLayout,
            )
        }
        entry<AppDestination.FeedArticles> { key ->
            val feed = appDrawerState.feeds.firstOrNull { it.id == key.feedId }
            ArticlesRoute(
                scope = ArticleScope.ForFeed(key.feedId),
                title = key.title,
                faviconBaseUrl = feed?.siteUrl?.takeIf { it.isNotBlank() } ?: feed?.url,
                onOpenArticles = ::openArticles,
                onNavigateToFeeds = { navigateTop(AppDestination.Feeds()) },
                onAddFeed = openAddFeed,
                onOpenDrawer = openDrawer,

                showNavigationIcon = !wideLayout,
            )
        }
        entry<AppDestination.Feeds> { key ->
            FeedsRoute(
                onNavigateToArticles = ::openScope,
                onNavigateToSettings = { navigateTop(AppDestination.Settings) },
                onOpenDrawer = openDrawer,

                showNavigationIcon = !wideLayout,
                openAddFeed = key.openAddFeed,
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

                showNavigationIcon = !wideLayout,
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

    val onNavigateFromDrawer: (AppDestination) -> Unit = { destination ->
        coroutineScope.launch { drawerState.close() }
        navigateTop(destination)
    }
    if (wideLayout) {
        PermanentNavigationDrawer(
            drawerContent = {
                AppPermanentDrawer(
                    state = appDrawerState,
                    currentDestination = currentDestination as? AppDestination,
                    onNavigate = onNavigateFromDrawer,
                )
            },
        ) {
            navContent()
        }
    } else {
        // On compact screens the drawer is modal; the reader keeps it closed because
        // the WebView consumes the edge drags that would open it.
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentDestination !is AppDestination.Reader,
            drawerContent = {
                AppDrawer(
                    state = appDrawerState,
                    currentDestination = currentDestination as? AppDestination,
                    onNavigate = onNavigateFromDrawer,
                )
            },
        ) {
            navContent()
        }
    }
}
