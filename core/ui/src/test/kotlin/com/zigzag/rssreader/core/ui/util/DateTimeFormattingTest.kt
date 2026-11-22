package com.zigzag.rssreader.core.ui.util

import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant

class DateTimeFormattingTest {

    @Test
    fun formatsUtcInstantInTargetTimezoneAndLocale() {
        val instant = Instant.parse("2026-11-23T08:30:00Z")

        val formatted = formatArticleDateTime(
            instant = instant,
            zoneId = ZoneId.of("America/New_York"),
            locale = Locale.US,
        )

        assertEquals("11/23/26, 3:30\u202FAM", formatted)
    }

    @Test
    fun localDateCanDifferFromUtcDate() {
        val instant = Instant.parse("2026-11-23T02:30:00Z")

        val formatted = formatArticleDateTime(
            instant = instant,
            zoneId = ZoneId.of("America/Los_Angeles"),
            locale = Locale.US,
        )

        assertEquals("11/22/26, 6:30\u202FPM", formatted)
    }

    @Test
    fun respectsLocaleSpecificFormatting() {
        val instant = Instant.parse("2026-11-23T08:30:00Z")

        val formatted = formatArticleDateTime(
            instant = instant,
            zoneId = ZoneId.of("Europe/Berlin"),
            locale = Locale.GERMANY,
        )

        assertEquals("23.11.26, 09:30", formatted)
    }
}
