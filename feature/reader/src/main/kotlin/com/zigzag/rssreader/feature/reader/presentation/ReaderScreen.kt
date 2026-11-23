package com.zigzag.rssreader.feature.reader.presentation

import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zigzag.rssreader.core.domain.model.Article
import com.zigzag.rssreader.core.ui.components.LoadingIndicator
import com.zigzag.rssreader.feature.reader.R

@Composable
fun ReaderRoute(articleId: String, onNavigateBack: () -> Unit) {
    val viewModel: ReaderViewModel = hiltViewModel()
    LaunchedEffect(articleId) { viewModel.load(articleId) }

    val article by viewModel.article.collectAsStateWithLifecycle()
    val html by viewModel.html.collectAsStateWithLifecycle()
    val link by viewModel.link.collectAsStateWithLifecycle()

    ReaderScreen(
        article = article,
        html = html,
        link = link,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
internal fun ReaderScreen(
    article: Article?,
    html: String?,
    link: String?,
    onNavigateBack: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var lastLoaded by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = article?.title ?: "Article",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) { Text("←") }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
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
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                settings.javaScriptEnabled = false
                                settings.domStorageEnabled = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.useWideViewPort = false
                                settings.loadWithOverviewMode = true
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
