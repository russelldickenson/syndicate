package com.zigzag.syndicate.feature.reader.presentation

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
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.domain.model.ArticleFont
import com.zigzag.syndicate.core.domain.model.ArticleFontSize
import com.zigzag.syndicate.core.ui.components.ArticleFontChipSelector
import com.zigzag.syndicate.core.ui.components.ArticleFontPreview
import com.zigzag.syndicate.core.ui.components.ArticleFontSizeSelector
import com.zigzag.syndicate.core.ui.components.LoadingIndicator
import com.zigzag.syndicate.core.ui.components.SectionHeader
import com.zigzag.syndicate.core.ui.components.articleFontFamily
import com.zigzag.syndicate.core.ui.components.formatArticleDate
import com.zigzag.syndicate.feature.reader.R
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
    val articleFontSize by viewModel.articleFontSize.collectAsStateWithLifecycle()

    ReaderScreen(
        article = article,
        html = html,
        link = link,
        articleFont = articleFont,
        articleFontSize = articleFontSize,
        canGoPrevious = hasPrevious,
        canGoNext = hasNext,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onNavigateBack = onNavigateBack,
        onSetFont = viewModel::setArticleFont,
        onSetFontSize = viewModel::setArticleFontSize,
    )
}

@Composable
internal fun ReaderScreen(
    article: Article?,
    html: String?,
    link: String?,
    articleFont: ArticleFont,
    articleFontSize: ArticleFontSize,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onNavigateBack: () -> Unit,
    onSetFont: (ArticleFont) -> Unit,
    onSetFontSize: (ArticleFontSize) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var showAppearanceSheet by remember { mutableStateOf(false) }
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
        ArticleFont.INTER -> com.zigzag.syndicate.core.ui.R.raw.font_inter
        ArticleFont.ATKINSON -> com.zigzag.syndicate.core.ui.R.raw.font_atkinson
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
    val titleFontFamily = articleFontFamily(articleFont)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        // IconButton centers its 24dp icon inside a 48dp touch target;
                        // offset it so the arrow lines up with the title and body text.
                        .offset(x = (-12).dp)
                        .semantics { contentDescription = "Back" },
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                IconButton(
                    onClick = { showAppearanceSheet = true },
                    modifier = Modifier
                        .offset(x = 12.dp)
                        .semantics { contentDescription = "Reading font and size" },
                ) { Icon(Icons.Filled.FormatSize, contentDescription = null) }
            }

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
                            val loadKey = "$target|$articleFont|$articleFontSize|$dark"
                            if (lastLoaded != loadKey) {
                                isLoading = true
                                if (!html.isNullOrBlank()) {
                                    webView.loadDataWithBaseURL(
                                        link,
                                        prepareArticleHtml(
                                            html.orEmpty(),
                                            dark = dark,
                                            font = articleFont,
                                            fontSize = articleFontSize,
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

    if (showAppearanceSheet) {
        ReadingAppearanceSheet(
            font = articleFont,
            fontSize = articleFontSize,
            onFontChange = onSetFont,
            onFontSizeChange = onSetFontSize,
            onDismiss = { showAppearanceSheet = false },
        )
    }
}

@Composable
private fun ReadingAppearanceSheet(
    font: ArticleFont,
    fontSize: ArticleFontSize,
    onFontChange: (ArticleFont) -> Unit,
    onFontSizeChange: (ArticleFontSize) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Reading appearance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SectionHeader("Font")
            ArticleFontChipSelector(selected = font, onSelect = onFontChange)
            SectionHeader("Text size")
            ArticleFontSizeSelector(selected = fontSize, onSelect = onFontSizeChange)
            ArticleFontPreview(font = font, size = fontSize)
        }
    }
}
