package app.fitdash.ui

import app.fitdash.health.SourceStatus
import app.fitdash.metrics.Band
import app.fitdash.metrics.Component
import app.fitdash.metrics.Good
import app.fitdash.metrics.RangeStatus
import app.fitdash.metrics.Suggestion
import app.fitdash.metrics.SuggestionKind
import app.fitdash.metrics.rangeStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

fun bandWord(band: Band) = when (band) {
    Band.RECOVER -> "Recover"
    Band.STEADY -> "Steady"
    Band.PRIMED -> "Primed"
}

fun bandAdvice(band: Band) = when (band) {
    Band.RECOVER -> "Your body's asking for rest. Keep it light today."
    Band.STEADY -> "A normal day. Moderate effort is fine."
    Band.PRIMED -> "Slept well and recovered. Good day to push."
}

private val COMPONENT_NAMES = mapOf(Component.HRV to "HRV", Component.RESTING_HR to "resting HR", Component.SLEEP to "sleep")

fun basedOn(used: Set<Component>): String? =
    if (used.size == Component.entries.size) null
    else "Based on " + Component.entries.filter { it in used }.joinToString(" + ") { COMPONENT_NAMES.getValue(it) }

fun formatDuration(d: Duration): String {
    val h = d.toHours()
    val m = d.toMinutes() % 60
    return if (h == 0L) "${m}m" else "${h}h ${m}m"
}

fun formatSteps(n: Long): String = String.format(Locale.ENGLISH, "%,d", n)

fun formatNumber(v: Double): String =
    if (abs(v - v.roundToLong()) < 0.05) v.roundToLong().toString() else String.format(Locale.ENGLISH, "%.1f", v)

private val DATE = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

fun formatDate(d: LocalDate): String = DATE.format(d)

private val APP_NAMES = mapOf(
    "com.fitbit.FitbitMobile" to "Fitbit",
    "com.google.android.apps.fitness" to "Google Fit",
    "com.google.android.apps.healthdata" to "Health Connect",
    "com.sec.android.app.shealth" to "Samsung Health",
)

fun sourceLine(s: SourceStatus): String = when {
    !s.granted -> "no access"
    !s.hasData -> "no data"
    else -> "✓ " + s.apps.joinToString(", ") { APP_NAMES[it] ?: it }
}

fun suggestionText(s: Suggestion): String = when (s.kind) {
    SuggestionKind.HARD_WORKOUT -> "Good day for a hard workout."
    SuggestionKind.MODERATE -> "Moderate effort today. Keep workouts under an hour."
    SuggestionKind.REST -> "Rest or an easy walk today."
    SuggestionKind.EARLIER_BED -> "Get to bed 30 minutes earlier tonight."
    SuggestionKind.EASY_ON_STIMULANTS -> "Go easy on caffeine and alcohol today."
    SuggestionKind.WALK -> "${formatSteps(s.stepsLeft)} steps to go. A 15-minute walk does it."
}

fun rangeNote(value: Double, range: ClosedFloatingPointRange<Double>?, good: Good): String {
    if (range == null) return "building your range"
    val above = value > range.endInclusive
    return when (rangeStatus(value, range, good)) {
        RangeStatus.IN -> "in your normal range"
        RangeStatus.GOOD_SIDE -> if (above) "above your range · good" else "below your range · good"
        RangeStatus.BAD_SIDE -> if (above) "above your normal range" else "below your normal range"
    }
}

fun sleepWord(score: Int) = when {
    score >= 85 -> "Restful"
    score >= 70 -> "Good"
    score >= 50 -> "Fair"
    else -> "Poor"
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

fun formatClock(t: Instant, zone: ZoneId): String = CLOCK.format(t.atZone(zone))

fun dayLetter(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
