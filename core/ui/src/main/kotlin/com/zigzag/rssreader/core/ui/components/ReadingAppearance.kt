package com.zigzag.rssreader.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zigzag.rssreader.core.domain.model.ArticleFont
import com.zigzag.rssreader.core.domain.model.ArticleFontSize
import com.zigzag.rssreader.core.ui.R

@Composable
fun articleFontFamily(font: ArticleFont): FontFamily =
    when (font) {
        ArticleFont.SANS_SERIF -> FontFamily.SansSerif
        ArticleFont.SERIF -> FontFamily.Serif
        ArticleFont.INTER -> FontFamily(Font(R.raw.font_inter, FontWeight.Normal, FontStyle.Normal))
        ArticleFont.ATKINSON ->
            FontFamily(Font(R.raw.font_atkinson, FontWeight.Normal, FontStyle.Normal))
    }

fun articleFontSizeSp(size: ArticleFontSize): TextUnit =
    when (size) {
        ArticleFontSize.SMALL -> 14.sp
        ArticleFontSize.MEDIUM -> 16.sp
        ArticleFontSize.LARGE -> 20.sp
    }

@Composable
fun ArticleFontSizeSelector(
    selected: ArticleFontSize,
    onSelect: (ArticleFontSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        ArticleFontSize.entries.forEach { option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = option.ordinal,
                    count = ArticleFontSize.entries.size,
                ),
            ) { Text(option.label) }
        }
    }
}

private fun ArticleFont.shortLabel(): String = when (this) {
    ArticleFont.SANS_SERIF -> "System Sans"
    ArticleFont.SERIF -> "System Serif"
    ArticleFont.INTER -> "Inter"
    ArticleFont.ATKINSON -> "Atkinson"
}

@Composable
fun ArticleFontChipSelector(
    selected: ArticleFont,
    onSelect: (ArticleFont) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ArticleFont.entries.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option.shortLabel()) },
            )
        }
    }
}

/** A sample of the selected font at the selected reading size. */
@Composable
fun ArticleFontPreview(
    font: ArticleFont,
    size: ArticleFontSize,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = "The quick brown fox jumps over the lazy dog",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = articleFontFamily(font),
                fontSize = articleFontSizeSp(size),
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp),
        )
    }
}
