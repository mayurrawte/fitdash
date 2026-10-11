package app.fitdash.ui

import app.fitdash.health.SourceStatus
import app.fitdash.metrics.ActivitySummary
import app.fitdash.metrics.Band
import app.fitdash.metrics.Component
import app.fitdash.metrics.HeroMode
import app.fitdash.metrics.ReadinessResult
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
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

fun eveningWord(goalPercent: Int) = when {
    goalPercent >= 100 -> "Goal done"
    goalPercent >= 75 -> "Almost there"
    else -> "Keep moving"
}

fun eveningAdvice(a: ActivitySummary): String =
    if (a.steps >= Tuning.STEP_GOAL) "Goal done. Wind down by 23:00 for a good tomorrow."
    else "${formatSteps(Tuning.STEP_GOAL - a.steps)} steps to go. Wind down by 23:00 for tomorrow."

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

fun miniScore(data: TodayData): Int? = when (data.hero) {
    HeroMode.READINESS -> (data.readiness as? ReadinessResult.Ready)?.score
    HeroMode.ACTIVITY -> data.activity?.goalPercent
}

fun miniLine(data: TodayData): String = when (data.hero) {
    HeroMode.READINESS -> when (val r = data.readiness) {
        is ReadinessResult.Ready -> "${r.score} · ${bandWord(r.band)}"
        is ReadinessResult.Calibrating -> "Calibrating"
        ReadinessResult.NoData -> "No data yet"
    }
    HeroMode.ACTIVITY -> data.activity?.let { "${formatSteps(it.steps)} steps · ${eveningWord(it.goalPercent)}" } ?: "No steps yet"
}
