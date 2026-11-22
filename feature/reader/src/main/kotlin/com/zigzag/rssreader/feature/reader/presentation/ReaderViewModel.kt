package com.zigzag.rssreader.feature.reader.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zigzag.rssreader.core.domain.model.Article
import com.zigzag.rssreader.core.domain.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private val currentArticleId = MutableStateFlow<String?>(null)
    private val markedRead = mutableSetOf<String>()

    val article: StateFlow<Article?> =
        currentArticleId
            .flatMapLatest { id ->
                if (id == null) flowOf(null) else articleRepository.observeArticle(id)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = null,
            )

    val html: StateFlow<String?> =
        article
            .map { it?.contentHtml }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = null,
            )

    val link: StateFlow<String?> =
        article
            .map { it?.link }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = null,
            )

    init {
        savedStateHandle.get<String>(KEY_ARTICLE_ID)?.let(::load)
    }

    fun load(articleId: String) {
        if (articleId.isBlank() || currentArticleId.value == articleId) return
        currentArticleId.value = articleId
        savedStateHandle[KEY_ARTICLE_ID] = articleId
        if (markedRead.add(articleId)) {
            viewModelScope.launch {
                runCatching { articleRepository.markRead(articleId) }
            }
        }
    }

    private companion object {
        const val KEY_ARTICLE_ID = "articleId"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
