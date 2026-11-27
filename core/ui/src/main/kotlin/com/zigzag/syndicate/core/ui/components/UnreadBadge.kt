package com.zigzag.syndicate.core.ui.components

import androidx.compose.ui.res.pluralStringResource
import com.zigzag.syndicate.core.ui.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    if (count > 0) {
        val description = pluralStringResource(R.plurals.core_ui_cd_unread_count, count, count)
        Badge(
            modifier = modifier.semantics { contentDescription = description },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(text = if (count > 99) stringResource(R.string.core_ui_unread_overflow) else count.toString())
        }
    }
}
