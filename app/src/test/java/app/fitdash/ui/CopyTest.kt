package app.fitdash.ui

import app.fitdash.health.SourceStatus
import app.fitdash.metrics.Component
import app.fitdash.metrics.Good
import app.fitdash.metrics.Suggestion
import app.fitdash.metrics.SuggestionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CopyTest {
    @Test fun `based-on line lists what was used`() {
        assertEquals("Based on resting HR + sleep", basedOn(setOf(Component.SLEEP, Component.RESTING_HR)))
        assertNull(basedOn(Component.entries.toSet()))
    }

    @Test fun `durations and numbers`() {
        assertEquals("7h 12m", formatDuration(Duration.ofMinutes(432)))
        assertEquals("45m", formatDuration(Duration.ofMinutes(45)))
        assertEquals("8,240", formatSteps(8240))
        assertEquals("94", formatNumber(94.0))
        assertEquals("14.5", formatNumber(14.5))
    }

    @Test fun `source lines`() {
        assertEquals("no access", sourceLine(SourceStatus("Weight", false, false, emptySet())))
        assertEquals("no data", sourceLine(SourceStatus("Weight", true, false, emptySet())))
        assertEquals("✓ Fitbit", sourceLine(SourceStatus("Sleep", true, true, setOf("com.fitbit.FitbitMobile"))))
        assertEquals("✓ Phone", sourceLine(SourceStatus("Steps", true, true, setOf("com.android.healthconnect.phone", "com.android.healthconnect.phone.steps"))))
    }

    @Test fun `days of data`() {
        assertEquals("1 day of data", daysOfData(1))
        assertEquals("12 days of data", daysOfData(12))
    }

    @Test fun `suggestion texts`() {
        assertEquals("Good day for a hard workout.", suggestionText(Suggestion(SuggestionKind.HARD_WORKOUT)))
        assertEquals("Moderate effort today. Keep workouts under an hour.", suggestionText(Suggestion(SuggestionKind.MODERATE)))
        assertEquals("Rest or an easy walk today.", suggestionText(Suggestion(SuggestionKind.REST)))
        assertEquals("Get to bed 30 minutes earlier tonight.", suggestionText(Suggestion(SuggestionKind.EARLIER_BED)))
        assertEquals("Go easy on caffeine and alcohol today.", suggestionText(Suggestion(SuggestionKind.EASY_ON_STIMULANTS)))
        assertEquals("2,000 steps to go. A 15-minute walk does it.", suggestionText(Suggestion(SuggestionKind.WALK, 2000)))
        assertEquals("2,001 steps to go before bed.", suggestionText(Suggestion(SuggestionKind.WALK, 2001)))
    }

    @Test fun `range notes`() {
        assertEquals("building your range", rangeNote(48.0, null, Good.HIGHER))
        assertEquals("in your normal range", rangeNote(42.0, 40.0..46.0, Good.HIGHER))
        assertEquals("above your range · good", rangeNote(48.0, 40.0..46.0, Good.HIGHER))
        assertEquals("below your range · good", rangeNote(56.0, 58.0..62.0, Good.LOWER))
        assertEquals("above your normal range", rangeNote(0.6, -0.3..0.3, Good.MIDDLE))
        assertEquals("below your normal range", rangeNote(93.0, 95.0..98.0, Good.HIGHER))
    }

    @Test fun `sleep words, clock and day letters`() {
        assertEquals("Restful", sleepWord(85))
        assertEquals("Good", sleepWord(70))
        assertEquals("Fair", sleepWord(50))
        assertEquals("Poor", sleepWord(49))
        assertEquals("23:10", formatClock(Instant.parse("2026-10-10T17:40:00Z"), ZoneId.of("Asia/Kolkata")))
        assertEquals("S", dayLetter(LocalDate.of(2026, 10, 11)))
        assertEquals("M", dayLetter(LocalDate.of(2026, 10, 12)))
    }
}

class IssueUrlTest {
    @Test fun `issue links carry title, label and device line but no health data`() {
        val bug = issueUrl(true, "0.5.0", "Google Pixel 8", "16")
        assertTrue(bug.startsWith("https://github.com/mayurrawte/fitdash/issues/new?title=Bug%3A%20&labels=bug&body="))
        assertTrue(bug.contains("fitdash%200.5.0%20%C2%B7%20Google%20Pixel%208%20%C2%B7%20Android%2016"))
        val idea = issueUrl(false, "0.5.0", "Google Pixel 8", "16")
        assertTrue(idea.contains("labels=enhancement"))
        assertTrue(idea.contains("title=Feature%20request%3A%20"))
    }
}
