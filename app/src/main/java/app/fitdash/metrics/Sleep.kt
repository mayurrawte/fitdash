package app.fitdash.metrics

import app.fitdash.health.SleepSession
import app.fitdash.health.SleepStage
import app.fitdash.health.StageType
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min
import kotlin.math.roundToInt

data class SleepSummary(
    val asleep: Duration,
    val deep: Duration,
    val rem: Duration,
    val light: Duration,
    val awake: Duration,
    val hasStages: Boolean,
    val score: Int,
    val stages: List<SleepStage>,
)

fun nightEnding(date: LocalDate, sessions: List<SleepSession>, zone: ZoneId): SleepSession? =
    sessions
        .filter { it.end.atZone(zone).toLocalDate() == date && Duration.between(it.start, it.end).seconds >= Tuning.MIN_NIGHT_HOURS * 3600 }
        .maxByOrNull { Duration.between(it.start, it.end) }

private fun SleepSession.total(vararg types: StageType): Duration =
    stages.filter { it.type in types }.fold(Duration.ZERO) { acc, s -> acc + Duration.between(s.start, s.end) }

private fun SleepSession.hasStages() =
    stages.any { it.type == StageType.DEEP || it.type == StageType.REM || it.type == StageType.LIGHT }

fun asleepDuration(night: SleepSession): Duration =
    if (night.stages.isEmpty()) Duration.between(night.start, night.end)
    else night.total(StageType.LIGHT, StageType.DEEP, StageType.REM, StageType.SLEEPING)

fun deepRemShare(night: SleepSession): Double? {
    if (!night.hasStages()) return null
    val asleep = asleepDuration(night).seconds.toDouble()
    if (asleep <= 0) return null
    return night.total(StageType.DEEP, StageType.REM).seconds / asleep
}

fun baselineDeepRemShare(date: LocalDate, sessions: List<SleepSession>, zone: ZoneId): Double? =
    median((1..Tuning.BASELINE_DAYS).mapNotNull { nightEnding(date.minusDays(it), sessions, zone)?.let(::deepRemShare) })

fun sleepScore(night: SleepSession, baselineShare: Double?): Int {
    val hours = asleepDuration(night).seconds / 3600.0
    var score = min(hours / Tuning.SLEEP_TARGET_HOURS, 1.0) * 100
    val share = deepRemShare(night)
    if (share != null && baselineShare != null) {
        score += ((share - baselineShare) * 100).coerceIn(-Tuning.STAGE_ADJUST_MAX, Tuning.STAGE_ADJUST_MAX)
    }
    return score.roundToInt().coerceIn(0, 100)
}

fun summarizeSleep(date: LocalDate, sessions: List<SleepSession>, zone: ZoneId): SleepSummary? {
    val night = nightEnding(date, sessions, zone) ?: return null
    return SleepSummary(
        asleep = asleepDuration(night),
        deep = night.total(StageType.DEEP),
        rem = night.total(StageType.REM),
        light = night.total(StageType.LIGHT),
        awake = night.total(StageType.AWAKE),
        hasStages = night.hasStages(),
        score = sleepScore(night, baselineDeepRemShare(date, sessions, zone)),
        stages = night.stages,
    )
}
