package app.fitdash.ui

import app.fitdash.health.HealthSnapshot
import app.fitdash.metrics.ReadinessResult
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
import app.fitdash.metrics.asleepDuration
import app.fitdash.metrics.dailyLast
import app.fitdash.metrics.dailyMean
import app.fitdash.metrics.nightEnding
import app.fitdash.metrics.readiness
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)

fun aiSummary(snapshot: HealthSnapshot, data: TodayData, now: ZonedDateTime): String {
    val zone = now.zone
    val date = now.toLocalDate()
    val hrv = dailyMean(snapshot.hrv, zone)
    val rhr = dailyLast(snapshot.restingHr, zone)

    val recovery = when (val r = data.readiness) {
        is ReadinessResult.Ready -> "${r.score} (${bandWord(r.band)})"
        is ReadinessResult.Calibrating -> "calibrating (night ${r.nights} of ${Tuning.MIN_NIGHTS})"
        ReadinessResult.NoData -> "no data"
    }
    val sleep = data.sleep?.let { "${formatDuration(it.asleep)} (score ${it.score})" } ?: "no data"
    val h = data.heart
    val hrvPart = h?.hrv?.let { v -> "${v.roundToInt()} ms" + (h.hrvBaseline?.let { " (baseline ${it.roundToInt()})" } ?: "") } ?: "no data"
    val rhrPart = h?.restingHr?.let { v -> "${v.roundToInt()} bpm" + (h.restingHrBaseline?.let { " (baseline ${it.roundToInt()})" } ?: "") } ?: "no data"
    val steps = data.activity?.let { formatSteps(it.steps) } ?: "no data"

    val rows = (Tuning.BASELINE_DAYS - 1 downTo 0L).map { date.minusDays(it) }.joinToString("\n") { d ->
        val score = (readiness(snapshot, d, zone) as? ReadinessResult.Ready)?.score?.toString()
        val sleepHours = nightEnding(d, snapshot.sleep, zone)?.let {
            String.format(Locale.ENGLISH, "%.1f", asleepDuration(it).seconds / 3600.0)
        }
        val daySteps = snapshot.dailyActivity.firstOrNull { it.date == d }?.steps?.toString()
        listOf(d.toString(), score, sleepHours, hrv[d]?.roundToInt()?.toString(), rhr[d]?.roundToInt()?.toString(), daySteps)
            .joinToString(", ") { it ?: "" }
    }

    val other = data.generic.joinToString(", ") { "${it.label} ${formatNumber(it.points.last().value)} ${it.unit}" }

    return buildString {
        append("My health data from fitdash (Health Connect), generated ${STAMP.format(now)}.\n")
        append("Please help me understand it. Baselines are my 30-day medians.\n\n")
        append("Today: recovery $recovery, sleep $sleep, HRV $hrvPart, resting HR $rhrPart, steps so far $steps.\n\n")
        append("Last 30 days (date, recovery, sleep_hours, hrv_ms, resting_hr, steps):\n")
        append(rows)
        append("\n")
        if (other.isNotEmpty()) append("\nOther data (latest): $other")
    }
}
