package app.fitdash.health

import java.time.Instant
import java.time.LocalDate

data class Sample(val time: Instant, val value: Double)

enum class StageType { AWAKE, LIGHT, DEEP, REM, SLEEPING }

data class SleepStage(val start: Instant, val end: Instant, val type: StageType)

data class SleepSession(val start: Instant, val end: Instant, val stages: List<SleepStage>)

data class HourBucket(val start: Instant, val steps: Long)

data class DayActivity(val date: LocalDate, val steps: Long, val distanceMeters: Double, val kcal: Double)

data class Interval(val start: Instant, val end: Instant)

data class GenericSeries(val label: String, val unit: String, val points: List<Sample>)

data class SourceStatus(val label: String, val granted: Boolean, val hasData: Boolean, val apps: Set<String>)

data class HealthSnapshot(
    val sleep: List<SleepSession> = emptyList(),
    val hrv: List<Sample> = emptyList(),
    val restingHr: List<Sample> = emptyList(),
    val heartRate: List<Sample> = emptyList(),
    val spo2: List<Sample> = emptyList(),
    val respiratoryRate: List<Sample> = emptyList(),
    val skinTempDelta: List<Sample> = emptyList(),
    val hourlySteps: List<HourBucket> = emptyList(),
    val dailyActivity: List<DayActivity> = emptyList(),
    val exercise: List<Interval> = emptyList(),
    val generic: List<GenericSeries> = emptyList(),
    val sources: List<SourceStatus> = emptyList(),
    val estimatedRestingHrDates: Set<LocalDate> = emptySet(),
    val waterDaily: Map<LocalDate, Double> = emptyMap(),
    val heightMeters: Double? = null,
    val weightKg: Double? = null,
)
