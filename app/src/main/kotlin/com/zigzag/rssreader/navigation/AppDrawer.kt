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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zigzag.rssreader.core.ui.components.AppIcon
import com.zigzag.rssreader.core.ui.components.SectionHeader
import com.zigzag.rssreader.core.ui.components.UnreadBadge

/**
 * The vertical navigation list: Feeds (All, Unread, every feed, manage), Tags,
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
                    glyph = "☰",
                    selected = currentDestination == AppDestination.ArticlesAll,
                    onClick = { onNavigate(AppDestination.ArticlesAll) },
                )
            }
            item(key = "unread") {
                DrawerRow(
                    label = "Unread",
                    glyph = "★",
                    badge = state.unreadCounts.global,
                    selected = currentDestination == AppDestination.ArticlesUnread,
                    onClick = { onNavigate(AppDestination.ArticlesUnread) },
                )
            }
            items(items = state.feeds, key = { "feed_${it.id}" }) { feed ->
                DrawerRow(
                    label = feed.title,
                    glyph = "•",
                    badge = state.unreadCounts.perFeed[feed.id] ?: 0,
                    selected = (currentDestination as? AppDestination.FeedArticles)?.feedId == feed.id,
                    onClick = { onNavigate(AppDestination.FeedArticles(feed.id, feed.title)) },
                )
            }
            item(key = "manage_feeds") {
                DrawerRow(
                    label = "Manage feeds",
                    glyph = "⚑",
                    selected = currentDestination == AppDestination.Feeds,
                    onClick = { onNavigate(AppDestination.Feeds) },
                )
            }

            if (state.tags.isNotEmpty()) {
                item(key = "tags_header") {
                    SectionHeader("Tags")
                }
                items(items = state.tags, key = { "tag_${it.id}" }) { tag ->
                    DrawerRow(
                        label = tag.name,
                        glyph = "#",
                        badge = state.unreadCounts.perTag[tag.id] ?: 0,
                        selected = (currentDestination as? AppDestination.TagArticles)?.tagId == tag.id,
                        onClick = { onNavigate(AppDestination.TagArticles(tag.id, tag.name)) },
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
                    glyph = "⚙",
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
    glyph: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Int = 0,
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
            Text(
                text = glyph,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp),
            )
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
