package com.zigzag.rssreader.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zigzag.rssreader.core.ui.components.AppIcon
import com.zigzag.rssreader.core.ui.components.FaviconImage
import com.zigzag.rssreader.core.ui.components.faviconCandidates
import com.zigzag.rssreader.core.ui.components.SectionHeader
import com.zigzag.rssreader.core.ui.components.UnreadBadge

/**
 * The vertical navigation list: Feeds (All, Unread, every feed, manage), Folders,
 * and Settings in its own section at the bottom.
 */
@Composable
fun AppDrawer(
    state: DrawerViewModelState,
    currentDestination: AppDestination?,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "RSS Reeder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            item(key = "feeds_header") {
                SectionHeader("Feeds")
            }
            item(key = "all") {
                DrawerRow(
                    label = "All",
                    icon = Icons.Filled.Menu,
                    selected = currentDestination == AppDestination.ArticlesAll,
                    onClick = { onNavigate(AppDestination.ArticlesAll) },
                )
            }
            item(key = "unread") {
                DrawerRow(
                    label = "Unread",
                    icon = Icons.Filled.Star,
                    badge = state.unreadCounts.global,
                    selected = currentDestination == AppDestination.ArticlesUnread,
                    onClick = { onNavigate(AppDestination.ArticlesUnread) },
                )
            }
            items(items = state.feeds, key = { "feed_${it.id}" }) { feed ->
                val candidates = remember(feed.siteUrl, feed.url) {
                    faviconCandidates(feed.siteUrl?.takeIf { it.isNotBlank() } ?: feed.url)
                }
                DrawerRow(
                    label = feed.title,
                    icon = Icons.Filled.Article,
                    favicon = candidates,
                    badge = state.unreadCounts.perFeed[feed.id] ?: 0,
                    selected = (currentDestination as? AppDestination.FeedArticles)?.feedId == feed.id,
                    onClick = { onNavigate(AppDestination.FeedArticles(feed.id, feed.title)) },
                )
            }
            item(key = "manage_feeds") {
                DrawerRow(
                    label = "Manage feeds",
                    icon = Icons.Filled.Flag,
                    selected = currentDestination == AppDestination.Feeds,
                    onClick = { onNavigate(AppDestination.Feeds) },
                )
            }

            if (state.folders.isNotEmpty()) {
                item(key = "folders_header") {
                    SectionHeader("Folders")
                }
                items(items = state.folders, key = { "folder_${it.id}" }) { folder ->
                    DrawerRow(
                        label = folder.name,
                        icon = Icons.Filled.Folder,
                        badge = state.unreadCounts.perFolder[folder.id] ?: 0,
                        selected = (currentDestination as? AppDestination.FolderArticles)?.folderId == folder.id,
                        onClick = { onNavigate(AppDestination.FolderArticles(folder.id, folder.name)) },
                    )
                }
            }

            item(key = "settings_divider") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item(key = "settings") {
                DrawerRow(
                    label = "Settings",
                    icon = Icons.Filled.Settings,
                    selected = currentDestination == AppDestination.Settings,
                    onClick = { onNavigate(AppDestination.Settings) },
                )
            }
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DrawerRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Int = 0,
    favicon: Pair<String, String>? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (favicon != null) {
                FaviconImage(
                    primaryUrl = favicon.first,
                    fallbackUrl = favicon.second,
                    fallback = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.width(24.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.width(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            UnreadBadge(count = badge)
        }
    }
}
