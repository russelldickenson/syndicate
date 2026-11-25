package com.zigzag.syndicate.feature.feeds.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface FeedsDestination : NavKey {
    @Serializable
    data object FeedList : FeedsDestination
}
