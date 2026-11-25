package com.zigzag.syndicate.core.network

import com.zigzag.syndicate.core.common.DispatcherProvider
import com.zigzag.syndicate.core.network.model.ParsedFeed
import javax.inject.Inject
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class FeedFetcher @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val parser: FeedParser,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun fetch(url: String): ParsedFeed = withContext(dispatchers.io) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Syndicate/1.0")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.string() ?: error("Empty feed body")
            parser.parse(body)
        }
    }
}
