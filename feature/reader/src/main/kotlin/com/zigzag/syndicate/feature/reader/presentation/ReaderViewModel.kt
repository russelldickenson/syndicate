package com.zigzag.syndicate.feature.reader.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.domain.model.ArticleFont
import com.zigzag.syndicate.core.domain.model.ArticleFontSize
import com.zigzag.syndicate.core.domain.repository.ArticleRepository
import com.zigzag.syndicate.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val articleFont: StateFlow<ArticleFont> =
        settingsRepository.observeSettings()
            .map { it.articleFont }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ArticleFont.SANS_SERIF,
            )

    val articleFontSize: StateFlow<ArticleFontSize> =
        settingsRepository.observeSettings()
            .map { it.articleFontSize }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ArticleFontSize.MEDIUM,
            )

    fun setArticleFont(font: ArticleFont) {
        viewModelScope.launch {
            runCatching { settingsRepository.updateSettings { it.copy(articleFont = font) } }
        }
    }

    fun setArticleFontSize(size: ArticleFontSize) {
        viewModelScope.launch {
            runCatching { settingsRepository.updateSettings { it.copy(articleFontSize = size) } }
        }
    }

    private val articleIds = MutableStateFlow<List<String>>(emptyList())
    private val _index = MutableStateFlow(0)
    private val markedRead = mutableSetOf<String>()

    private val currentArticleId: StateFlow<String?> =
        combine(articleIds, _index) { ids, index -> ids.getOrNull(index) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = null,
            )

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

    val hasPrevious: StateFlow<Boolean> =
        combine(articleIds, _index) { ids, index -> ids.isNotEmpty() && index > 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = false,
            )

    val hasNext: StateFlow<Boolean> =
        combine(articleIds, _index) { ids, index -> ids.isNotEmpty() && index < ids.lastIndex }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = false,
            )

    fun initialize(ids: List<String>, index: Int) {
        if (articleIds.value == ids && _index.value == index) return
        articleIds.value = ids
        _index.value = if (ids.isEmpty()) 0 else index.coerceIn(ids.indices)
        markCurrentRead()
    }

    fun previous() = moveTo(_index.value - 1)

    fun next() = moveTo(_index.value + 1)

    private fun moveTo(index: Int) {
        if (index !in articleIds.value.indices) return
        _index.value = index
        markCurrentRead()
    }

    private fun markCurrentRead() {
        val id = articleIds.value.getOrNull(_index.value) ?: return
        if (markedRead.add(id)) {
            viewModelScope.launch {
                runCatching { articleRepository.markRead(id) }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
