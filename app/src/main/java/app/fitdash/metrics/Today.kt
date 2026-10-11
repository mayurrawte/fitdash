package app.fitdash.metrics

import app.fitdash.health.GenericSeries
import app.fitdash.health.HealthSnapshot
import app.fitdash.health.SourceStatus
import java.time.LocalDate
import java.time.ZonedDateTime

enum class HeroMode { READINESS, ACTIVITY }

enum class CardKind { SLEEP, HEART, VITALS, ACTIVITY, READINESS_COMPACT, WEEK, GENERIC, SOURCES }

data class WeekDay(val date: LocalDate, val score: Int?)

data class TodayData(
    val hero: HeroMode,
    val date: LocalDate,
    val readiness: ReadinessResult,
    val sleep: SleepSummary?,
    val heart: HeartSummary?,
    val vitals: List<VitalReading>,
    val activity: ActivitySummary?,
    val week: List<WeekDay>,
    val generic: List<GenericSeries>,
    val sources: List<SourceStatus>,
    val cards: List<CardKind>,
)

private val MORNING = listOf(
    CardKind.SLEEP, CardKind.HEART, CardKind.VITALS, CardKind.ACTIVITY, CardKind.WEEK, CardKind.GENERIC, CardKind.SOURCES,
)
private val EVENING = listOf(
    CardKind.ACTIVITY, CardKind.READINESS_COMPACT, CardKind.SLEEP, CardKind.HEART, CardKind.VITALS,
    CardKind.WEEK, CardKind.GENERIC, CardKind.SOURCES,
)

fun buildToday(snapshot: HealthSnapshot, now: ZonedDateTime): TodayData {
    val zone = now.zone
    val date = now.toLocalDate()
    val evening = now.hour >= Tuning.EVENING_HOUR
    val readiness = readiness(snapshot, date, zone)
    val sleep = summarizeSleep(date, snapshot.sleep, zone)
    val heart = heartSummary(snapshot, date, zone)
    val vitals = vitals(snapshot, date, zone)
    val activity = activitySummary(snapshot, if (evening) date else date.minusDays(1), zone)
    val week = (6L downTo 0L).map { d ->
        val day = date.minusDays(d)
        WeekDay(day, (readiness(snapshot, day, zone) as? ReadinessResult.Ready)?.score)
    }
    val generic = snapshot.generic.filter { it.points.isNotEmpty() }

    val available = mapOf(
        CardKind.SLEEP to (sleep != null),
        CardKind.HEART to (heart != null),
        CardKind.VITALS to vitals.isNotEmpty(),
        CardKind.ACTIVITY to (activity != null),
        CardKind.READINESS_COMPACT to (readiness is ReadinessResult.Ready),
        CardKind.WEEK to week.any { it.score != null },
        CardKind.GENERIC to generic.isNotEmpty(),
        CardKind.SOURCES to true,
    )

    return TodayData(
        hero = if (evening) HeroMode.ACTIVITY else HeroMode.READINESS,
        date = date,
        readiness = readiness,
        sleep = sleep,
        heart = heart,
        vitals = vitals,
        activity = activity,
        week = week,
        generic = generic,
        sources = snapshot.sources,
        cards = (if (evening) EVENING else MORNING).filter { available.getValue(it) },
    )
}
