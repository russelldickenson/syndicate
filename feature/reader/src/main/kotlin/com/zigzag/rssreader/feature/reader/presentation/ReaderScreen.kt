package com.zigzag.rssreader.feature.reader.presentation

import android.content.Intent
import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.rssreader.core.domain.model.Article
import com.zigzag.rssreader.core.ui.components.ArticleFavicon
import com.zigzag.rssreader.core.ui.components.faviconCandidates
import com.zigzag.rssreader.core.ui.components.formatArticleDate
import com.zigzag.rssreader.core.ui.components.LoadingIndicator
import com.zigzag.rssreader.feature.reader.R
import kotlin.math.abs

@Composable
fun ReaderRoute(
    articleIds: List<String>,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
) {
    val viewModel: ReaderViewModel = hiltViewModel()
    LaunchedEffect(articleIds, initialIndex) { viewModel.initialize(articleIds, initialIndex) }

    val article by viewModel.article.collectAsStateWithLifecycle()
    val html by viewModel.html.collectAsStateWithLifecycle()
    val link by viewModel.link.collectAsStateWithLifecycle()
    val hasPrevious by viewModel.hasPrevious.collectAsStateWithLifecycle()
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle()

    ReaderScreen(
        article = article,
        html = html,
        link = link,
        canGoPrevious = hasPrevious,
        canGoNext = hasNext,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
internal fun ReaderScreen(
    article: Article?,
    html: String?,
    link: String?,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var lastLoaded by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Keep the touch listener stable while always calling the current callbacks.
    val previousAction = rememberUpdatedState(onPrevious)
    val nextAction = rememberUpdatedState(onNext)

    val openOriginal: () -> Unit = {
        if (!link.isNullOrBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }
    }

    val shareArticle: () -> Unit = {
        if (!link.isNullOrBlank()) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, article?.title.orEmpty())
                putExtra(Intent.EXTRA_TEXT, link)
            }
            runCatching {
                context.startActivity(Intent.createChooser(sendIntent, null))
            }
        }
    }

    val target = when {
        !html.isNullOrBlank() -> "html:$html"
        !link.isNullOrBlank() -> "url:$link"
        else -> null
    }

    LaunchedEffect(article, target) {
        if (article != null && target == null) isLoading = false
    }

    val dateLabel = article?.publishedAt?.let(::formatArticleDate).orEmpty()
    val faviconBaseUrl = article?.feedSiteUrl?.takeIf { it.isNotBlank() } ?: article?.link
    val faviconUrls = remember(faviconBaseUrl) { faviconCandidates(faviconBaseUrl) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ArticleFavicon(
                            primaryUrl = faviconUrls?.first,
                            fallbackUrl = faviconUrls?.second,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp)),
                        )
                        if (faviconUrls != null) {
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = article?.title.orEmpty().ifBlank { "Article" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (article != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = listOfNotNull(
                                        article.feedTitle.takeIf { it.isNotBlank() },
                                        dateLabel.takeIf { it.isNotBlank() },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = canGoPrevious,
                        modifier = Modifier.semantics { contentDescription = "Previous article" },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.feature_reader_ic_previous),
                            contentDescription = null,
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = canGoNext,
                        modifier = Modifier.semantics { contentDescription = "Next article" },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.feature_reader_ic_next),
                            contentDescription = null,
                        )
                    }
                    IconButton(
                        onClick = openOriginal,
                        enabled = !link.isNullOrBlank(),
                        modifier = Modifier.semantics { contentDescription = "View original article" },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.feature_reader_ic_open_original),
                            contentDescription = null,
                        )
                    }
                    IconButton(
                        onClick = shareArticle,
                        enabled = !link.isNullOrBlank(),
                        modifier = Modifier.semantics { contentDescription = "Share article" },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.feature_reader_ic_share),
                            contentDescription = null,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (article == null) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (target != null) {
                key(target) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { webContext ->
                                val swipeDetector = GestureDetector(
                                    webContext,
                                    object : GestureDetector.SimpleOnGestureListener() {
                                        override fun onFling(
                                            start: MotionEvent?,
                                            end: MotionEvent,
                                            velocityX: Float,
                                            velocityY: Float,
                                        ): Boolean {
                                            if (start == null) return false
                                            val dx = end.x - start.x
                                            val dy = end.y - start.y
                                            if (abs(dx) > abs(dy) && abs(dx) > 80 && abs(velocityX) > 150) {
                                                if (dx > 0) previousAction.value() else nextAction.value()
                                                return true
                                            }
                                            return false
                                        }
                                    },
                                )
                            WebView(webContext).apply {
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                settings.javaScriptEnabled = false
                                settings.domStorageEnabled = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.useWideViewPort = false
                                settings.loadWithOverviewMode = true
                                setOnTouchListener { _, event -> swipeDetector.onTouchEvent(event) }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        update = { webView ->
                            if (lastLoaded != target) {
                                isLoading = true
                                if (!html.isNullOrBlank()) {
                                    webView.loadDataWithBaseURL(
                                        link,
                                        prepareArticleHtml(html.orEmpty()),
                                        "text/html",
                                        "UTF-8",
                                        null,
                                    )
                                } else if (!link.isNullOrBlank()) {
                                    webView.loadUrl(link.orEmpty())
                                }
                                lastLoaded = target
                            }
                        },
                    )
                }
            }
        }

            if (isLoading && article != null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }
        }
    }
}
