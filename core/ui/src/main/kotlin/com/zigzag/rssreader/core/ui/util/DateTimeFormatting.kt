package com.zigzag.rssreader.core.ui.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.datetime.Instant

/**
 * Formats a UTC instant as a short, localized date and time in [zoneId].
 *
 * The defaults use the device timezone and locale; both can be overridden to keep
 * formatting deterministic in tests.
 */
internal fun formatArticleDateTime(
    instant: Instant,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(zoneId)
    return formatter.format(java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()))
}
