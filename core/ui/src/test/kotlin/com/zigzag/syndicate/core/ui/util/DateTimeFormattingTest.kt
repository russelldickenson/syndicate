package com.zigzag.syndicate.core.ui.util

import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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

        assertEquals("11/23/26, 3:30 AM", formatted.normalizeSpaces())
    }

    @Test
    fun localDateCanDifferFromUtcDate() {
        val instant = Instant.parse("2026-11-23T02:30:00Z")

        val formatted = formatArticleDateTime(
            instant = instant,
            zoneId = ZoneId.of("America/Los_Angeles"),
            locale = Locale.US,
        )

        assertEquals("11/22/26, 6:30 PM", formatted.normalizeSpaces())
    }

    @Test
    fun respectsLocaleSpecificFormatting() {
        val instant = Instant.parse("2026-11-23T08:30:00Z")

        val formatted = formatArticleDateTime(
            instant = instant,
            zoneId = ZoneId.of("Europe/Berlin"),
            locale = Locale.GERMANY,
        )

        assertEquals("23.11.26, 09:30", formatted.normalizeSpaces())
    }

    @Test
    fun australianLocaleUsesDayFirstOrder() {
        val formatted = formatArticleDateTime(
            instant = Instant.parse("2026-06-23T02:30:00Z"),
            zoneId = ZoneId.of("Australia/Sydney"),
            locale = Locale.forLanguageTag("en-AU"),
        )

        assertTrue(formatted.startsWith("23/"), formatted)
        assertTrue(formatted.contains("pm"), formatted)
    }

    // CLDR uses either a plain space or a narrow no-break space before AM/PM
    // depending on the JDK's locale data.
    private fun String.normalizeSpaces(): String = replace('\u202F', ' ')
}
