package com.zigzag.rssreader.feature.reader.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ReaderDestination(val articleId: String) : NavKey
