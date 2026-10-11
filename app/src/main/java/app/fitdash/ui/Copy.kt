package app.fitdash.ui

import app.fitdash.health.SourceStatus
import app.fitdash.metrics.Band
import app.fitdash.metrics.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
