package app.fitdash.metrics

import app.fitdash.health.GenericSeries
import app.fitdash.health.HealthSnapshot
import app.fitdash.health.SourceStatus
import java.time.LocalDate
import java.time.ZonedDateTime

data class WeekDay(val date: LocalDate, val score: Int?)

data class SleepDay(val date: LocalDate, val hours: Double?, val score: Int?)

data class ActivityDay(val date: LocalDate, val steps: Long?, val distanceKm: Double?, val kcal: Double?)

data class TodayData(
    val date: LocalDate,
    val readiness: ReadinessResult,
    val sleep: SleepSummary?,
    val heart: HeartSummary?,
    val vitals: List<VitalReading>,
    val activity: ActivitySummary?,
    val movementScore: Int?,
    val readinessWeek: List<WeekDay>,
    val sleepWeek: List<SleepDay>,
    val activityWeek: List<ActivityDay>,
    val suggestions: List<Suggestion>,
    val generic: List<GenericSeries>,
    val sources: List<SourceStatus>,
)

fun buildToday(snapshot: HealthSnapshot, now: ZonedDateTime): TodayData {
    val zone = now.zone
    val date = now.toLocalDate()
    val days = (Tuning.WEEK_DAYS - 1 downTo 0L).map { date.minusDays(it) }
    val readiness = readiness(snapshot, date, zone)
    val sleep = summarizeSleep(date, snapshot.sleep, zone)
    val heart = heartSummary(snapshot, date, zone)
    val activity = activitySummary(snapshot, date, zone)
    return TodayData(
        date = date,
        readiness = readiness,
        sleep = sleep,
        heart = heart,
        vitals = vitals(snapshot, date, zone),
        activity = activity,
        movementScore = activity?.goalPercent?.coerceAtMost(100),
        readinessWeek = days.map { WeekDay(it, (readiness(snapshot, it, zone) as? ReadinessResult.Ready)?.score) },
        sleepWeek = days.map { d ->
            val s = summarizeSleep(d, snapshot.sleep, zone)
            SleepDay(d, s?.asleep?.let { it.seconds / 3600.0 }, s?.score)
        },
        activityWeek = days.map { d ->
            val a = snapshot.dailyActivity.firstOrNull { it.date == d }
            ActivityDay(d, stepsOn(snapshot, d, zone), a?.distanceMeters?.div(1000), a?.kcal)
        },
        suggestions = suggestions(readiness, sleep, heart, activity, now.hour),
        generic = snapshot.generic.filter { it.points.isNotEmpty() },
        sources = snapshot.sources,
    )
}
