package app.fitdash.metrics

import app.fitdash.health.HealthSnapshot
import app.fitdash.health.Sample
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

enum class MetricKind {
    RECOVERY, SLEEP_SCORE, SLEEP_HOURS, DEEP, REM, LIGHT, AWAKE,
    RESTING_HR, HRV, SPO2, RESPIRATORY, SKIN_TEMP,
    STEPS, DISTANCE, CALORIES, ACTIVE_MINUTES,
}

data class DayValue(val date: LocalDate, val value: Double?)

private fun window(date: LocalDate) = (Tuning.BASELINE_DAYS - 1 downTo 0L).map { date.minusDays(it) }

fun metricSeries(snapshot: HealthSnapshot, kind: MetricKind, date: LocalDate, zone: ZoneId, goals: Goals): List<DayValue> {
    fun sleep(d: LocalDate) = summarizeSleep(d, snapshot.sleep, zone, goals.sleepHours)
    fun stage(d: LocalDate, pick: (SleepSummary) -> Duration) = sleep(d)?.takeIf { it.hasStages }?.let { pick(it).seconds / 60.0 }
    val dailyMeans = when (kind) {
        MetricKind.HRV -> dailyMean(snapshot.hrv, zone)
        MetricKind.SPO2 -> dailyMean(snapshot.spo2, zone)
        MetricKind.RESPIRATORY -> dailyMean(snapshot.respiratoryRate, zone)
        MetricKind.SKIN_TEMP -> dailyMean(snapshot.skinTempDelta, zone)
        MetricKind.RESTING_HR -> dailyLast(snapshot.restingHr, zone)
        else -> emptyMap()
    }
    return window(date).map { d ->
        val value = when (kind) {
            MetricKind.RECOVERY -> (readiness(snapshot, d, zone, goals) as? ReadinessResult.Ready)?.score?.toDouble()
            MetricKind.SLEEP_SCORE -> sleep(d)?.score?.toDouble()
            MetricKind.SLEEP_HOURS -> sleep(d)?.let { it.asleep.seconds / 3600.0 }
            MetricKind.DEEP -> stage(d) { it.deep }
            MetricKind.REM -> stage(d) { it.rem }
            MetricKind.LIGHT -> stage(d) { it.light }
            MetricKind.AWAKE -> stage(d) { it.awake }
            MetricKind.RESTING_HR, MetricKind.HRV, MetricKind.SPO2, MetricKind.RESPIRATORY, MetricKind.SKIN_TEMP -> dailyMeans[d]
            MetricKind.STEPS -> stepsOn(snapshot, d, zone)?.toDouble()
            MetricKind.DISTANCE -> snapshot.dailyActivity.firstOrNull { it.date == d }?.let { it.distanceMeters / 1000 }
            MetricKind.CALORIES -> snapshot.dailyActivity.firstOrNull { it.date == d }?.kcal
            MetricKind.ACTIVE_MINUTES -> activitySummary(snapshot, d, zone, goals.steps)?.activeMinutes?.toDouble()
        }
        DayValue(d, value)
    }
}

fun genericDailySeries(samples: List<Sample>, date: LocalDate, zone: ZoneId): List<DayValue> {
    val daily = dailyMean(samples, zone)
    return window(date).map { DayValue(it, daily[it]) }
}
