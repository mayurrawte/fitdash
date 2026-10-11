package app.fitdash.ui

import app.fitdash.health.DayActivity
import app.fitdash.health.GenericSeries
import app.fitdash.health.HealthSnapshot
import app.fitdash.health.HourBucket
import app.fitdash.health.Interval
import app.fitdash.health.Sample
import app.fitdash.health.SleepSession
import app.fitdash.health.SleepStage
import app.fitdash.health.SourceStatus
import app.fitdash.health.StageType
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.buildToday
import app.fitdash.ui.today.TodayUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object PreviewData {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 11)
    private fun at(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).atZone(zone).toInstant()

    private val hourly = listOf(0, 0, 0, 0, 0, 0, 200, 800, 1200, 400, 300, 600, 900, 300, 200, 1100, 800, 400, 300, 0, 0, 0, 0, 0)

    private fun stagedNight(start: Instant): SleepSession {
        val plan = listOf(
            StageType.LIGHT to 20, StageType.DEEP to 40, StageType.LIGHT to 30, StageType.REM to 25, StageType.AWAKE to 10,
            StageType.LIGHT to 45, StageType.DEEP to 40, StageType.REM to 35, StageType.LIGHT to 50, StageType.REM to 37,
        )
        var t = start
        val stages = plan.map { (type, min) -> val e = t.plusSeconds(min * 60L); SleepStage(t, e, type).also { t = e } }
        return SleepSession(start, t, stages)
    }

    private val snapshot by lazy {
        val days = (0..13).map { today.minusDays(it.toLong()) }
        HealthSnapshot(
            sleep = days.map { stagedNight(at(it.minusDays(1), 23, 10)) },
            hrv = days.mapIndexed { i, d -> Sample(at(d, 5), if (i == 0) 48.0 else 40.0 + i % 5) },
            restingHr = days.mapIndexed { i, d -> Sample(at(d, 7), if (i == 0) 58.0 else 60.0 + i % 3) },
            spo2 = days.map { Sample(at(it, 4), 96.0) },
            respiratoryRate = days.map { Sample(at(it, 4), 14.5) },
            hourlySteps = hourly.mapIndexed { h, s -> HourBucket(at(today, h), s.toLong()) },
            dailyActivity = listOf(
                DayActivity(today.minusDays(1), 5760, 4300.0, 1820.0),
                DayActivity(today, 8240, 6100.0, 2140.0),
            ),
            exercise = listOf(Interval(at(today, 18), at(today, 18, 46))),
            generic = listOf(GenericSeries("Weight", "kg", days.reversed().mapIndexed { i, d -> Sample(at(d, 8), 72.4 - i * 0.05) })),
            sources = listOf(
                SourceStatus("Sleep", true, true, setOf("com.fitbit.FitbitMobile")),
                SourceStatus("Blood glucose", true, false, emptySet()),
                SourceStatus("Weight", false, false, emptySet()),
            ),
        )
    }

    fun today(hour: Int, minute: Int = 0): TodayData = buildToday(snapshot, today.atTime(hour, minute).atZone(zone))

    fun ready(hour: Int, minute: Int = 0): TodayUiState.Ready {
        val now = today.atTime(hour, minute).atZone(zone)
        val data = buildToday(snapshot, now)
        return TodayUiState.Ready(data, aiSummary(snapshot, data, now))
    }
}
