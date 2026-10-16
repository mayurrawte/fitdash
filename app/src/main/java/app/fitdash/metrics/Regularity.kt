package app.fitdash.metrics

import app.fitdash.health.SleepSession
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class NightSpan(val date: LocalDate, val start: Instant?, val end: Instant?)

data class Regularity(
    val sri: Int,
    val nightsUsed: Int,
    val pairsUsed: Int,
    val bedtimeSpreadMin: Int?,
    val wakeSpreadMin: Int?,
    val nights: List<NightSpan>,
)

val PHILLIPS = Source("Phillips et al. 2017", "Irregular sleep/wake patterns are associated with poorer academic performance and delayed circadian and sleep/wake timing. Sci Rep 7:3216. Defines the Sleep Regularity Index.")
val WINDRED = Source("Windred et al. 2023", "Sleep regularity is a stronger predictor of mortality risk than sleep duration: a prospective cohort study. Sleep 47(1). UK Biobank, 60 977 adults.")

const val REGULARITY_NIGHTS = 14L
const val MIN_PAIRS = 5
private const val SPREAD_NIGHTS = 7
private const val DAY_MINUTES = 24 * 60

fun regularityWordBand(sri: Int): Int = when {
    sri >= 80 -> 0
    sri >= 70 -> 1
    sri >= 60 -> 2
    else -> 3
}

// Minutes since 18:00 of the previous evening, so bedtimes either side of midnight sit on one scale.
private fun minutesFromEvening(t: Instant, zone: ZoneId): Int {
    val z = t.atZone(zone)
    val m = z.hour * 60 + z.minute
    return if (m >= 18 * 60) m - 18 * 60 else m + 6 * 60
}

// Sample standard deviation (n − 1) of the clock time, in minutes.
fun clockSpreadMinutes(times: List<Instant>, zone: ZoneId): Int? {
    if (times.size < 3) return null
    val xs = times.map { minutesFromEvening(it, zone).toDouble() }
    val mean = xs.average()
    return sqrt(xs.sumOf { (it - mean) * (it - mean) } / (xs.size - 1)).roundToInt()
}

const val NIGHT_AXIS_START_MIN = 20 * 60   // the chart axis runs 20:00 to 12:00
const val NIGHT_AXIS_SPAN_MIN = 16 * 60

// Where a clock time sits on the 20:00–12:00 night axis, 0..1. Times outside it pin to the nearer edge, measured on the
// same evening-first scale as the spreads, so 19:45 pins left and 13:00 pins right.
fun nightAxisFraction(minuteOfDay: Int): Float {
    val sinceEvening = if (minuteOfDay >= 18 * 60) minuteOfDay - 18 * 60 else minuteOfDay + 6 * 60
    val sinceStart = sinceEvening - (NIGHT_AXIS_START_MIN - 18 * 60)
    return (sinceStart.coerceIn(0, NIGHT_AXIS_SPAN_MIN).toFloat() / NIGHT_AXIS_SPAN_MIN)
}

private data class Pairing(val nights: List<NightSpan>, val matches: Long, val pairs: Long, val dayPairs: Int)

private fun pair(sessions: List<SleepSession>, date: LocalDate, zone: ZoneId): Pairing {
    val dates = window(date, REGULARITY_NIGHTS)
    val mains = dates.associateWith { nightEnding(it, sessions, zone) }
    val nights = dates.map { d -> NightSpan(d, mains[d]?.start, mains[d]?.end) }
    val present = mains.filterValues { it != null }.keys
    if (present.size < 2) return Pairing(nights, 0, 0, 0)
    val first = sessions.minOf { it.start }
    val last = sessions.maxOf { it.end }
    // Minute index from `first`; a minute is asleep when inside any session. Session end is exclusive.
    val total = Duration.between(first, last).toMinutes().toInt() + 1
    val asleep = BooleanArray(total)
    for (s in sessions) {
        val a = Duration.between(first, s.start).toMinutes().toInt().coerceAtLeast(0)
        val b = Duration.between(first, s.end).toMinutes().toInt().coerceAtMost(total)
        for (i in a until b) asleep[i] = true
    }
    var matches = 0L
    var pairs = 0L
    var dayPairs = 0
    for (d in dates) {
        val next = d.plusDays(1)
        if (d !in present || next !in present) continue
        dayPairs++
        // The sleep day d runs from 18:00 the evening before to 18:00 on d, so the night ending d sits inside it; each
        // minute is paired with the one exactly 24 h later. Minutes outside the recorded span (before the first session,
        // after the last) are unknown rather than awake and are skipped.
        val base = Duration.between(first, d.minusDays(1).atTime(18, 0).atZone(zone).toInstant()).toMinutes()
        for (m in 0 until DAY_MINUTES) {
            val i = base + m
            val j = i + DAY_MINUTES
            if (i < 0 || j >= total) continue
            pairs++
            if (asleep[i.toInt()] == asleep[j.toInt()]) matches++
        }
    }
    return Pairing(nights, matches, pairs, dayPairs)
}

fun regularityProgress(sessions: List<SleepSession>, date: LocalDate, zone: ZoneId): Int = pair(sessions, date, zone).dayPairs

fun regularity(sessions: List<SleepSession>, date: LocalDate, zone: ZoneId): FitnessValue<Regularity>? {
    val p = pair(sessions, date, zone)
    val dayPairs = p.dayPairs
    if (dayPairs < MIN_PAIRS || p.pairs == 0L) return null
    val sri = (200.0 * p.matches / p.pairs - 100).roundToInt().coerceIn(0, 100)
    val recent = p.nights.takeLast(SPREAD_NIGHTS)
    val bed = clockSpreadMinutes(recent.mapNotNull { it.start }, zone)
    val wake = clockSpreadMinutes(recent.mapNotNull { it.end }, zone)
    val nightsUsed = p.nights.count { it.start != null }
    return FitnessValue(
        Regularity(sri, nightsUsed, dayPairs, bed, wake, p.nights),
        Explanation(
            formula = "SRI = 200 × (share of minute pairs 24 h apart in the same state, asleep or awake) − 100, over the last 14 nights",
            inputs = listOfNotNull(
                "Nights used" to "$nightsUsed of 14",
                "Day pairs used" to dayPairs.toString(),
                bed?.let { "Bedtime" to "± $it min over 7 nights" },
                wake?.let { "Wake time" to "± $it min over 7 nights" },
            ),
            source = PHILLIPS,
            note = "Bands are fitdash's choice: 80 and over very regular, 70–79 regular, 60–69 varies, under 60 irregular. Regularity predicted mortality more strongly than duration in ${WINDRED.title} (${WINDRED.citation}).",
        ),
    )
}
