package com.zigzag.syndicate.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/** Adds a localized accessibility label without resolving it inside a semantics lambda. */
@Composable
fun Modifier.contentDescriptionRes(@StringRes resId: Int, vararg formatArgs: Any): Modifier {
    val description = stringResource(resId, *formatArgs)
    return this.semantics { contentDescription = description }
}

/** Adds a localized state description without resolving it inside a semantics lambda. */
@Composable
fun Modifier.stateDescriptionRes(@StringRes resId: Int, vararg formatArgs: Any): Modifier {
    val description = stringResource(resId, *formatArgs)
    return this.semantics { stateDescription = description }
}
