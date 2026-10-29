package app.fitdash.metrics

import java.time.Instant
import java.time.ZonedDateTime

fun morningAlert(vitals: List<VitalReading>, heart: HeartSummary?): List<VitalReading> {
    val readings = listOfNotNull(
        heart?.restingHr?.let { VitalReading("Resting HR", "bpm", it, heart.restingHrBaseline, heart.restingHrRange, Good.LOWER) },
        heart?.hrv?.let { VitalReading("HRV", "ms", it, heart.hrvBaseline, heart.hrvRange, Good.HIGHER) },
    ) + vitals
    return readings.filter { it.range != null && rangeStatus(it.value, it.range, it.good) == RangeStatus.BAD_SIDE }
}

fun morningAlertDue(now: ZonedDateTime, lastSent: Instant?): Boolean {
    if (now.hour !in 6..10) return false
    return lastSent == null || lastSent.atZone(now.zone).toLocalDate() != now.toLocalDate()
}
