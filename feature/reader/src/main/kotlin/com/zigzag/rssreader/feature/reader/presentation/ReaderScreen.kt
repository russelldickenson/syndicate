package com.zigzag.rssreader.feature.reader.presentation

import android.content.Intent
import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.util.Base64
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.rssreader.core.domain.model.Article
import com.zigzag.rssreader.core.domain.model.ArticleFont
import com.zigzag.rssreader.core.ui.components.LoadingIndicator
import com.zigzag.rssreader.core.ui.components.formatArticleDate
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
    val articleFont by viewModel.articleFont.collectAsStateWithLifecycle()

    ReaderScreen(
        article = article,
        html = html,
        link = link,
        articleFont = articleFont,
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
    articleFont: ArticleFont,
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
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
        }
    }

    val shareArticle: () -> Unit = {
        if (!link.isNullOrBlank()) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, article?.title.orEmpty())
                putExtra(Intent.EXTRA_TEXT, link)
            }
            runCatching { context.startActivity(Intent.createChooser(sendIntent, null)) }
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
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Custom fonts are inlined into the HTML as a Base64 data URI so the WebView
    // can render them even though the article is loaded from a feed URL.
    val fontRes = when (articleFont) {
        ArticleFont.INTER -> R.raw.font_inter
        ArticleFont.ATKINSON_SANS, ArticleFont.ATKINSON_SERIF -> R.raw.font_atkinson
        else -> null
    }
    val embeddedFont = remember(fontRes) {
        fontRes?.let {
            Base64.encodeToString(
                context.resources.openRawResource(it).use { stream -> stream.readBytes() },
                Base64.NO_WRAP,
            )
        }
    }
    val atkinson = FontFamily(Font(R.raw.font_atkinson, FontWeight.Normal, FontStyle.Normal))
    val titleFontFamily = when (articleFont) {
        ArticleFont.SERIF -> FontFamily.Serif
        ArticleFont.INTER -> FontFamily(Font(R.raw.font_inter, FontWeight.Normal, FontStyle.Normal))
        ArticleFont.ATKINSON_SANS, ArticleFont.ATKINSON_SERIF -> atkinson
        ArticleFont.SANS_SERIF -> FontFamily.SansSerif
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(56.dp),
                containerColor = MaterialTheme.colorScheme.background,
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
                        Icon(painterResource(R.drawable.feature_reader_ic_previous), null)
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = canGoNext,
                        modifier = Modifier.semantics { contentDescription = "Next article" },
                    ) {
                        Icon(painterResource(R.drawable.feature_reader_ic_next), null)
                    }
                    IconButton(
                        onClick = openOriginal,
                        enabled = !link.isNullOrBlank(),
                        modifier = Modifier.semantics { contentDescription = "View original article" },
                    ) {
                        Icon(painterResource(R.drawable.feature_reader_ic_open_original), null)
                    }
                    IconButton(
                        onClick = shareArticle,
                        enabled = !link.isNullOrBlank(),
                        modifier = Modifier.semantics { contentDescription = "Share article" },
                    ) {
                        Icon(painterResource(R.drawable.feature_reader_ic_share), null)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    // IconButton centers its 24dp icon inside a 48dp touch target;
                    // offset it so the arrow lines up with the title and body text.
                    .offset(x = (-12).dp)
                    .semantics { contentDescription = "Back" },
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }

            if (article != null) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFontFamily,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dateLabel.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = titleFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (isLoading && article != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (target != null) {
                key(target, articleFont) {
                    AndroidView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
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
                            val loadKey = "$target|$articleFont|$dark"
                            if (lastLoaded != loadKey) {
                                isLoading = true
                                if (!html.isNullOrBlank()) {
                                    webView.loadDataWithBaseURL(
                                        link,
                                        prepareArticleHtml(
                                            html.orEmpty(),
                                            dark = dark,
                                            font = articleFont,
                                            embeddedFont = embeddedFont,
                                        ),
                                        "text/html",
                                        "UTF-8",
                                        null,
                                    )
                                } else if (!link.isNullOrBlank()) {
                                    webView.loadUrl(link.orEmpty())
                                }
                                lastLoaded = loadKey
                            }
                        },
                    )
                }
            } else if (article == null) {
                LoadingIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
