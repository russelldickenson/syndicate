package com.zigzag.syndicate.core.ui.components

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
        Badge(
            modifier = modifier.semantics { contentDescription = "$count unread articles" },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(text = if (count > 99) "99+" else count.toString())
        }
    }
}
