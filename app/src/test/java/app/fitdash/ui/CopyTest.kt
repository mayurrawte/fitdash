package app.fitdash.ui

import app.fitdash.health.SourceStatus
import app.fitdash.metrics.Component
import app.fitdash.metrics.FitnessCard
import app.fitdash.metrics.LoadBand
import app.fitdash.metrics.MissingInput
import app.fitdash.metrics.Status
import app.fitdash.metrics.regularityBand
import app.fitdash.ui.components.statusWord
import app.fitdash.metrics.Good
import app.fitdash.metrics.HeartSummary
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
        assertEquals("Short night", sleepWord(49))
        assertEquals("23:10", formatClock(Instant.parse("2026-10-10T17:40:00Z"), ZoneId.of("Asia/Kolkata")))
        assertEquals("S", dayLetter(LocalDate.of(2026, 10, 11)))
        assertEquals("M", dayLetter(LocalDate.of(2026, 10, 12)))
    }

    @Test fun greetingByHour() {
        assertEquals("Good morning", greeting(4))
        assertEquals("Good morning", greeting(11))
        assertEquals("Good afternoon", greeting(12))
        assertEquals("Good afternoon", greeting(16))
        assertEquals("Good evening", greeting(17))
        assertEquals("Good evening", greeting(3))
    }

    @Test fun streakLineOnlyFromThree() {
        assertNull(streakLine(2))
        assertEquals("3 days in a row", streakLine(3))
        assertEquals("12 days in a row", streakLine(12))
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

    private fun src(label: String, granted: Boolean = true, data: Boolean = true, apps: Set<String> = if (data) setOf("com.fitbit.FitbitMobile") else emptySet()) =
        SourceStatus(label, granted, data, apps)

    @Test fun `missing reason names the writing app`() {
        val sources = listOf(src("Sleep"), src("Heart rate variability", data = false), src("Weight", granted = false, data = false))
        val writers = writerNames(sources)
        assertEquals(listOf("Fitbit"), writers)
        assertEquals("Fitbit hasn't written any heart rate variability to Health Connect.", missingReason(sources[1], writers))
        assertTrue(missingReason(sources[2], writers)!!.startsWith("Access is off"))
        assertNull(missingReason(sources[0], writers))
        assertEquals("No app has written this to Health Connect yet.", missingReason(sources[1], emptyList()))
    }

    @Test fun `phone and health connect are not counted as watches`() {
        val sources = listOf(src("Steps", apps = setOf("com.android.healthconnect.phone.steps", "com.google.android.apps.healthdata")))
        assertEquals(emptyList<String>(), writerNames(sources))
    }

    @Test fun `recovery line says which signal is missing`() {
        val sleep = src("Sleep")
        assertNull(missingRecoveryLine(listOf(sleep, src("Heart rate variability"), src("Resting heart rate"))))
        assertEquals(
            "Fitbit isn't sharing HRV with Health Connect, so recovery uses resting heart rate and sleep.",
            missingRecoveryLine(listOf(sleep, src("Heart rate variability", data = false), src("Resting heart rate"))),
        )
        assertEquals(
            "Fitbit isn't sharing HRV or resting heart rate with Health Connect, so recovery uses sleep only.",
            missingRecoveryLine(listOf(sleep, src("Heart rate variability", data = false), src("Resting heart rate", data = false))),
        )
        assertEquals(
            "Allow HRV in Health Connect so recovery can use it.",
            missingRecoveryLine(listOf(sleep, src("Heart rate variability", granted = false, data = false), src("Resting heart rate"))),
        )
    }

    @Test fun `the system and fitdash itself are not named as writers`() {
        val sources = listOf(src("Sleep"), src("Hydration", apps = setOf("app.fitdash", "android")), src("SpO₂", data = false))
        assertEquals(listOf("Fitbit"), writerNames(sources))
        assertEquals("Fitbit hasn't written any SpO₂ to Health Connect.", missingReason(sources[2], writerNames(sources)))
    }

    @Test fun `fitness words`() {
        assertEquals("3 years younger", fitnessAgeWord(-3))
        assertEquals("1 year older", fitnessAgeWord(1))
        assertEquals("matches your age", fitnessAgeWord(0))
        assertEquals("ramping fast", loadWord(LoadBand.RAMPING))
        assertEquals("Add gender in Settings", missingWord(MissingInput.GENDER))
        assertEquals("Add height in Settings", missingWord(MissingInput.HEIGHT))
        assertEquals("Needs resting heart rate", missingWord(MissingInput.RESTING_HR))
        assertEquals("Fitness age", fitnessCardName(FitnessCard.FITNESS_AGE))
        assertEquals("3.5 h", formatHoursShort(3.5))
        assertEquals("0 h", formatHoursShort(0.0))
    }

    @Test fun `regularity and calm words`() {
        assertEquals("very regular", regularityWord(regularityBand(85)))
        assertEquals("regular", regularityWord(regularityBand(72)))
        assertEquals("varies", regularityWord(regularityBand(65)))
        assertEquals("irregular", regularityWord(regularityBand(40)))
        assertEquals("Short night", sleepWord(40))
        assertEquals("Good", sleepWord(70))
        assertEquals("BELOW YOUR USUAL", statusWord(Status.BELOW_USUAL))
    }

    @Test fun `morning readout reads sleep, one vital against your range, and the suggestion`() {
        fun heart(rhr: Double? = null, rhrRange: ClosedFloatingPointRange<Double>? = null, hrv: Double? = null, hrvRange: ClosedFloatingPointRange<Double>? = null) =
            HeartSummary(hrv, null, rhr, null, emptyList(), emptyList(), hrvRange, rhrRange)
        val sleep = Duration.ofMinutes(452)
        assertEquals(
            "Slept 7h 32m. Resting heart rate a little lower than your usual. Good day for a hard workout.",
            morningReadout(sleep, heart(rhr = 56.0, rhrRange = 58.0..64.0), Suggestion(SuggestionKind.HARD_WORKOUT)),
        )
        assertEquals(
            "Slept 6h 10m. HRV in your normal range. Get to bed 30 minutes earlier tonight.",
            morningReadout(Duration.ofMinutes(370), heart(hrv = 42.0, hrvRange = 38.0..50.0), Suggestion(SuggestionKind.EARLIER_BED)),
        )
        // The vital that is out of its range is the one worth saying.
        assertEquals("HRV lower than your usual.", morningReadout(null, heart(rhr = 60.0, rhrRange = 58.0..64.0, hrv = 30.0, hrvRange = 38.0..50.0), null))
        assertEquals("Resting heart rate in your normal range.", morningReadout(null, heart(rhr = 60.0, rhrRange = 58.0..64.0, hrv = 42.0, hrvRange = 38.0..50.0), null))
        assertEquals("Slept 7h 32m.", morningReadout(sleep, null, null))
        assertNull(morningReadout(null, null, null))
        assertEquals("Resting heart rate higher than your usual.", morningReadout(null, heart(rhr = 70.0, rhrRange = 58.0..64.0), null))
    }
}
