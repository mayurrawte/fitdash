package app.fitdash.ui

import app.fitdash.health.SourceStatus
import app.fitdash.metrics.Band
import app.fitdash.metrics.BmiBand
import app.fitdash.metrics.Component
import app.fitdash.metrics.Good
import app.fitdash.metrics.MetricKind
import app.fitdash.metrics.RangeStatus
import app.fitdash.metrics.Suggestion
import app.fitdash.metrics.SuggestionKind
import app.fitdash.metrics.Tuning
import app.fitdash.metrics.rangeStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
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

fun formatLiters(v: Double): String = String.format(Locale.ENGLISH, "%.2f", v).trimEnd('0').trimEnd('.')

fun formatBmi(v: Double): String = String.format(Locale.ENGLISH, "%.1f", v)

fun bmiWord(band: BmiBand) = when (band) {
    BmiBand.UNDER -> "UNDERWEIGHT"
    BmiBand.HEALTHY -> "HEALTHY"
    BmiBand.OVER -> "OVERWEIGHT"
    BmiBand.OBESE -> "OBESE"
}

fun formatHeight(cm: Int, miles: Boolean): String {
    if (!miles) return "$cm cm"
    val inches = (cm / 2.54).roundToInt()
    return "${inches / 12} ft ${inches % 12} in"
}

fun formatNumber(v: Double): String =
    if (abs(v - v.roundToLong()) < 0.05) v.roundToLong().toString() else String.format(Locale.ENGLISH, "%.1f", v)

private val DATE = DateTimeFormatter.ofPattern("EEE '·' d MMM", Locale.ENGLISH)

fun formatDate(d: LocalDate): String = DATE.format(d)

fun daysOfData(n: Int): String = if (n == 1) "1 day of data" else "$n days of data"

private val APP_NAMES = mapOf(
    "com.fitbit.FitbitMobile" to "Fitbit",
    "com.google.android.apps.fitness" to "Google Fit",
    "com.google.android.apps.healthdata" to "Health Connect",
    "com.sec.android.app.shealth" to "Samsung Health",
)

fun sourceLine(s: SourceStatus): String = when {
    !s.granted -> "no access"
    !s.hasData -> "no data"
    else -> "✓ " + s.apps.map { APP_NAMES[it] ?: if (it.startsWith("com.android.healthconnect.phone")) "Phone" else it }.distinct().joinToString(", ")
}

fun suggestionText(s: Suggestion): String = when (s.kind) {
    SuggestionKind.HARD_WORKOUT -> "Good day for a hard workout."
    SuggestionKind.MODERATE -> "Moderate effort today. Keep workouts under an hour."
    SuggestionKind.REST -> "Rest or an easy walk today."
    SuggestionKind.EARLIER_BED -> "Get to bed 30 minutes earlier tonight."
    SuggestionKind.EASY_ON_STIMULANTS -> "Go easy on caffeine and alcohol today."
    SuggestionKind.WALK ->
        if (s.stepsLeft <= Tuning.WALK_15_MIN_STEPS) "${formatSteps(s.stepsLeft)} steps to go. A 15-minute walk does it."
        else "${formatSteps(s.stepsLeft)} steps to go before bed."
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
private val CLOCK_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

fun formatTime12(t: Instant, zone: ZoneId): String = CLOCK_12.format(t.atZone(zone))

fun formatClock(t: Instant, zone: ZoneId): String = CLOCK.format(t.atZone(zone))

fun dayLetter(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)

fun metricName(kind: MetricKind) = when (kind) {
    MetricKind.RECOVERY -> "Recovery"
    MetricKind.SLEEP_SCORE -> "Sleep score"
    MetricKind.SLEEP_HOURS -> "Sleep"
    MetricKind.DEEP -> "Deep sleep"
    MetricKind.REM -> "REM sleep"
    MetricKind.LIGHT -> "Light sleep"
    MetricKind.AWAKE -> "Awake"
    MetricKind.RESTING_HR -> "Resting heart rate"
    MetricKind.HRV -> "HRV"
    MetricKind.SPO2 -> "SpO₂"
    MetricKind.RESPIRATORY -> "Respiratory rate"
    MetricKind.SKIN_TEMP -> "Skin temperature"
    MetricKind.STEPS -> "Steps"
    MetricKind.DISTANCE -> "Distance"
    MetricKind.CALORIES -> "Calories"
    MetricKind.ACTIVE_MINUTES -> "Active minutes"
    MetricKind.WATER -> "Water"
    MetricKind.BMI -> "BMI"
}

fun metricExplain(kind: MetricKind) = when (kind) {
    MetricKind.RECOVERY -> "A 0 to 100 blend of your HRV, resting heart rate and sleep compared with your own recent normal."
    MetricKind.SLEEP_SCORE -> "Mostly time asleep against your goal, adjusted a little for how much deep and REM sleep you got."
    MetricKind.SLEEP_HOURS -> "Time actually asleep, not counting time spent awake in bed."
    MetricKind.DEEP -> "The slowest, heaviest stage of sleep, in minutes per night."
    MetricKind.REM -> "The stage where most dreaming happens, in minutes per night."
    MetricKind.LIGHT -> "The lighter stage that makes up most of a typical night, in minutes."
    MetricKind.AWAKE -> "Minutes you were awake during the night after first falling asleep."
    MetricKind.RESTING_HR -> "Your heart rate at full rest, usually lowest during sleep. Lower than your normal is generally a good sign."
    MetricKind.HRV -> "The small variation in time between heartbeats. Higher than your normal is generally a good sign."
    MetricKind.SPO2 -> "The share of oxygen your blood is carrying, as measured by your wearable overnight."
    MetricKind.RESPIRATORY -> "How many breaths you take per minute while you sleep."
    MetricKind.SKIN_TEMP -> "How far your skin temperature sat from your own usual level overnight."
    MetricKind.STEPS -> "Steps counted across the day, compared with your daily goal."
    MetricKind.DISTANCE -> "How far you covered on foot during the day."
    MetricKind.CALORIES -> "Energy burned across the day, as reported by your tracker."
    MetricKind.ACTIVE_MINUTES -> "Minutes spent in recorded exercise sessions."
    MetricKind.WATER -> "Water you have logged for the day, compared with your daily goal."
    MetricKind.BMI -> "Your weight divided by your height squared. A general guide only, and it does not describe your health."
}

const val GENERIC_EXPLAIN = "The daily average of this reading from Health Connect."
