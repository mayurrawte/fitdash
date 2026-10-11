package app.fitdash.metrics

import app.fitdash.health.HealthSnapshot
import app.fitdash.health.Sample
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class HeartSummary(
    val hrv: Double?,
    val hrvBaseline: Double?,
    val restingHr: Double?,
    val restingHrBaseline: Double?,
    val hrvTrend: List<Double>,
    val restingHrTrend: List<Double>,
    val hrvRange: ClosedFloatingPointRange<Double>?,
    val restingHrRange: ClosedFloatingPointRange<Double>?,
)

data class VitalReading(
    val label: String,
    val unit: String,
    val value: Double,
    val baseline: Double?,
    val range: ClosedFloatingPointRange<Double>?,
    val good: Good,
)

data class ActivitySummary(
    val date: LocalDate,
    val steps: Long,
    val goalPercent: Int,
    val hourly: List<Long>,
    val activeMinutes: Long,
    val distanceKm: Double,
    val kcal: Double,
)

fun heartSummary(snapshot: HealthSnapshot, date: LocalDate, zone: ZoneId): HeartSummary? {
    val hrv = dailyMean(snapshot.hrv, zone)
    val rhr = dailyLast(snapshot.restingHr, zone)
    if (hrv[date] == null && rhr[date] == null) return null
    val from = date.minusDays(Tuning.TREND_DAYS - 1)
    fun trend(daily: Map<LocalDate, Double>) = daily.filterKeys { it in from..date }.toSortedMap().values.toList()
    return HeartSummary(
        hrv = hrv[date],
        hrvBaseline = baseline(hrv, date),
        restingHr = rhr[date],
        restingHrBaseline = baseline(rhr, date),
        hrvTrend = trend(hrv),
        restingHrTrend = trend(rhr),
        hrvRange = normalRange(hrv, date),
        restingHrRange = normalRange(rhr, date),
    )
}

private data class VitalSource(val label: String, val unit: String, val samples: List<Sample>, val good: Good)

fun vitals(snapshot: HealthSnapshot, date: LocalDate, zone: ZoneId): List<VitalReading> =
    listOf(
        VitalSource("SpO₂", "%", snapshot.spo2, Good.HIGHER),
        VitalSource("Respiratory rate", "br/min", snapshot.respiratoryRate, Good.MIDDLE),
        VitalSource("Skin temperature", "°C", snapshot.skinTempDelta, Good.MIDDLE),
    ).mapNotNull { s ->
        val daily = dailyMean(s.samples, zone)
        daily[date]?.let { VitalReading(s.label, s.unit, it, baseline(daily, date), normalRange(daily, date), s.good) }
    }

fun activitySummary(snapshot: HealthSnapshot, date: LocalDate, zone: ZoneId): ActivitySummary? {
    val day = snapshot.dailyActivity.firstOrNull { it.date == date }
    val buckets = snapshot.hourlySteps.filter { it.start.atZone(zone).toLocalDate() == date }
    if (day == null && buckets.isEmpty()) return null
    val hourly = List(24) { h -> buckets.filter { it.start.atZone(zone).hour == h }.sumOf { it.steps } }
    val steps = day?.steps ?: hourly.sum()
    val active = snapshot.exercise
        .filter { it.start.atZone(zone).toLocalDate() == date }
        .sumOf { Duration.between(it.start, it.end).toMinutes() }
    return ActivitySummary(
        date = date,
        steps = steps,
        goalPercent = (steps * 100.0 / Tuning.STEP_GOAL).roundToInt(),
        hourly = hourly,
        activeMinutes = active,
        distanceKm = (day?.distanceMeters ?: 0.0) / 1000,
        kcal = day?.kcal ?: 0.0,
    )
}
