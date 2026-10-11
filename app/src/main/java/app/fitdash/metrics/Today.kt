package app.fitdash.metrics

import app.fitdash.health.GenericSeries
import app.fitdash.health.HealthSnapshot
import app.fitdash.health.SleepSession
import app.fitdash.health.SourceStatus
import java.time.LocalDate
import java.time.ZonedDateTime

data class WeekDay(val date: LocalDate, val score: Int?)

data class SleepDay(val date: LocalDate, val hours: Double?, val score: Int?)

data class ActivityDay(val date: LocalDate, val steps: Long?, val distanceKm: Double?, val kcal: Double?)

data class TodayData(
    val date: LocalDate,
    val isToday: Boolean,
    val goals: Goals,
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
    val naps: List<SleepSession>,
    val previous: DayScores,
    val restingHrEstimated: Boolean,
    val trend: List<DayTrend>,
    val daysWithData: Set<LocalDate>,
    val estimatedRestingHrDates: Set<LocalDate>,
    val series: Map<MetricKind, List<DayValue>>,
    val genericDaily: Map<String, List<DayValue>>,
)

data class DayScores(val sleep: Int?, val readiness: Int?, val movement: Int?)

data class DayTrend(val date: LocalDate, val recovery: Int?, val sleepScore: Int?, val steps: Long?, val restingHr: Double?, val hrv: Double?)

fun buildToday(snapshot: HealthSnapshot, now: ZonedDateTime, date: LocalDate = now.toLocalDate(), goals: Goals = Goals(), augmented: Boolean = false): TodayData {
    val zone = now.zone
    val snap = if (augmented) snapshot else withEstimatedRestingHr(snapshot, zone, now.toLocalDate())
    val isToday = date == now.toLocalDate()
    val readiness = readiness(snap, date, zone, goals)
    val sleep = summarizeSleep(date, snap.sleep, zone, goals.sleepHours, snap.heartRate)
    val heart = heartSummary(snap, date, zone)
    val activity = activitySummary(snap, date, zone, goals.steps)
    val hrvDaily = dailyMean(snap.hrv, zone)
    val rhrDaily = dailyLast(snap.restingHr, zone)
    val trend = (Tuning.BASELINE_DAYS - 1 downTo 0L).map { date.minusDays(it) }.map { d ->
        DayTrend(
            date = d,
            recovery = (readiness(snap, d, zone, goals) as? ReadinessResult.Ready)?.score,
            sleepScore = summarizeSleep(d, snap.sleep, zone, goals.sleepHours)?.score,
            steps = stepsOn(snap, d, zone),
            restingHr = rhrDaily[d],
            hrv = hrvDaily[d],
        )
    }
    val prev = date.minusDays(1)
    val week = trend.takeLast(Tuning.WEEK_DAYS.toInt())
    return TodayData(
        date = date,
        isToday = isToday,
        goals = goals,
        readiness = readiness,
        sleep = sleep,
        heart = heart,
        vitals = vitals(snap, date, zone),
        activity = activity,
        movementScore = activity?.goalPercent?.coerceAtMost(100),
        readinessWeek = week.map { WeekDay(it.date, it.recovery) },
        sleepWeek = week.map { d ->
            val s = summarizeSleep(d.date, snap.sleep, zone, goals.sleepHours)
            SleepDay(d.date, s?.asleep?.let { it.seconds / 3600.0 }, s?.score)
        },
        activityWeek = week.map { d ->
            val a = snap.dailyActivity.firstOrNull { it.date == d.date }
            ActivityDay(d.date, d.steps, a?.distanceMeters?.div(1000), a?.kcal)
        },
        suggestions = if (isToday) suggestions(readiness, sleep, heart, activity, now.hour, goals.steps) else emptyList(),
        generic = snap.generic.filter { it.points.isNotEmpty() },
        sources = snap.sources,
        naps = napsEnding(date, snap.sleep, zone),
        previous = DayScores(
            summarizeSleep(prev, snap.sleep, zone, goals.sleepHours)?.score,
            trend.getOrNull(trend.size - 2)?.recovery,
            activitySummary(snap, prev, zone, goals.steps)?.goalPercent?.coerceAtMost(100),
        ),
        restingHrEstimated = date in snap.estimatedRestingHrDates,
        trend = trend,
        daysWithData = (snap.sleep.map { it.end.atZone(zone).toLocalDate() } +
            snap.dailyActivity.filter { it.steps > 0 }.map { it.date } +
            snap.hourlySteps.filter { it.steps > 0 }.map { it.start.atZone(zone).toLocalDate() }).toSet(),
        estimatedRestingHrDates = snap.estimatedRestingHrDates,
        series = MetricKind.entries.associateWith { metricSeries(snap, it, date, zone, goals) },
        genericDaily = snap.generic.filter { it.points.isNotEmpty() }.associate { it.label to genericDailySeries(it.points, date, zone) },
    )
}
