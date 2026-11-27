package com.zigzag.syndicate.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.domain.model.Feed
import com.zigzag.syndicate.core.domain.model.Folder
import com.zigzag.syndicate.core.ui.util.formatArticleDateTime
import java.net.URI

@Composable
fun FeedRow(feed: Feed, onFeedClick: () -> Unit, onEditFolder: () -> Unit) {
    val faviconBase = feed.siteUrl?.takeIf { it.isNotBlank() } ?: feed.url
    val faviconUrls = remember(faviconBase) { faviconCandidates(faviconBase) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFeedClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (faviconUrls != null) {
                FaviconImage(
                    primaryUrl = faviconUrls.first,
                    fallbackUrl = faviconUrls.second,
                    fallback = {},
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feed.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = hostFromUrl(feed.url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            UnreadBadge(count = feed.unreadCount)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onEditFolder) {
                Text(text = "Folder")
            }
        }
    }
}

@Composable
fun FolderRow(
    folder: Folder,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    selected: Boolean = false,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            UnreadBadge(count = folder.unreadCount)
            IconButton(onClick = onEdit, modifier = Modifier.semantics {
                contentDescription = "Edit folder ${folder.name}"
            }) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
        }
    }
}

@Composable
fun ArticleRow(article: Article, onRead: () -> Unit) {
    val titleStyle = if (article.isRead) {
        MaterialTheme.typography.bodyLarge
    } else {
        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    }.copy(lineHeight = 20.sp)
    val dateLocale = java.util.Locale.forLanguageTag(
        androidx.compose.ui.text.intl.Locale.current.toLanguageTag(),
    )
    val dateLabel = article.publishedAt
        ?.let { formatArticleDateTime(it, locale = dateLocale) }
        .orEmpty()
    val faviconBaseUrl = article.feedSiteUrl?.takeIf { it.isNotBlank() } ?: article.link
    val faviconUrls = remember(faviconBaseUrl) { faviconCandidates(faviconBaseUrl) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRead),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = article.title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (faviconUrls != null) {
                    ArticleFavicon(
                        primaryUrl = faviconUrls.first,
                        fallbackUrl = faviconUrls.second,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = article.feedTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dateLabel.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The favicon URLs to try for a site: `<scheme>://<host>/favicon.ico` first, then
 * DuckDuckGo's icon service, which covers sites that do not serve a root favicon.
 * Null when the site's host cannot be determined.
 */
fun faviconCandidates(baseUrl: String?): Pair<String, String>? {
    val primary = faviconUrl(baseUrl) ?: return null
    val fallback = iconServiceUrl(baseUrl) ?: return primary to primary
    return primary to fallback
}

/**
 * Loads a site's favicon, falling back to [fallback] if no favicon URL can be
 * built or neither the direct favicon nor the icon service loads.
 */
@Composable
fun FaviconImage(
    primaryUrl: String?,
    fallbackUrl: String?,
    fallback: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = primaryUrl
    if (start == null) {
        Box(modifier) { fallback() }
        return
    }
    var url by remember(start) { mutableStateOf(start) }
    var usedFallback by remember(start) { mutableStateOf(false) }
    var failed by remember(start) { mutableStateOf(false) }
    if (failed) {
        Box(modifier) { fallback() }
        return
    }
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error) {
                when {
                    !usedFallback && fallbackUrl != null && fallbackUrl != start -> {
                        usedFallback = true
                        url = fallbackUrl
                    }
                    else -> failed = true
                }
            }
        },
    )
}

@Composable
private fun ArticleFavicon(
    primaryUrl: String?,
    fallbackUrl: String?,
    modifier: Modifier = Modifier,
) {
    FaviconImage(
        primaryUrl = primaryUrl,
        fallbackUrl = fallbackUrl,
        fallback = {},
        modifier = modifier,
    )
}

fun formatArticleDate(
    instant: kotlinx.datetime.Instant,
    locale: java.util.Locale = java.util.Locale.getDefault(),
): String = formatArticleDateTime(instant, locale = locale)

private fun faviconUrl(baseUrl: String?): String? {
    val trimmed = baseUrl?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val host = hostOf(trimmed) ?: return null
    val scheme = if (trimmed.startsWith("http://", ignoreCase = true)) "http" else "https"
    return "$scheme://$host/favicon.ico"
}

private fun iconServiceUrl(baseUrl: String?): String? =
    hostOf(baseUrl)?.let { "https://icons.duckduckgo.com/ip3/$it.ico" }

private fun hostOf(baseUrl: String?): String? {
    val trimmed = baseUrl?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    return runCatching { URI(candidate) }
        .getOrNull()
        ?.host
        ?.takeIf { it.isNotBlank() }
}

private fun hostFromUrl(url: String): String =
    runCatching { URI(url).host }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: url
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
