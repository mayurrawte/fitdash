package app.fitdash.metrics

import app.fitdash.IST
import app.fitdash.TODAY
import app.fitdash.at
import app.fitdash.day
import app.fitdash.history
import app.fitdash.plus
import app.fitdash.health.DayActivity
import app.fitdash.health.GenericSeries
import app.fitdash.health.HealthSnapshot
import app.fitdash.health.HourBucket
import app.fitdash.health.Sample
import app.fitdash.health.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class TodayTest {
    private fun time(h: Int, m: Int = 0): ZonedDateTime = TODAY.atTime(h, m).atZone(IST)

    private val full = history(TODAY, 10, stages = true) +
        day(TODAY, hrv = 46.0, rhr = 58.0, sleepHours = 7.5, stages = true) +
        HealthSnapshot(
            spo2 = listOf(Sample(at(TODAY, 4), 95.0)),
            dailyActivity = listOf(
                DayActivity(TODAY.minusDays(1), 5760, 4300.0, 1820.0),
                DayActivity(TODAY, 8240, 6100.0, 2140.0),
            ),
            generic = listOf(GenericSeries("Weight", "kg", listOf(Sample(at(TODAY, 8), 72.4)))),
        )

    @Test fun `activity is always today and movement is capped`() {
        val t = buildToday(full, time(8, 10))
        assertEquals(TODAY, t.activity!!.date)
        assertEquals(92, t.movementScore)
        val big = HealthSnapshot(dailyActivity = listOf(DayActivity(TODAY, 12000, 9000.0, 2500.0)))
        assertEquals(100, buildToday(big, time(8)).movementScore)
    }

    @Test fun `week series keep empty days`() {
        val t = buildToday(full, time(8))
        listOf(t.readinessWeek.map { it.date }, t.sleepWeek.map { it.date }, t.activityWeek.map { it.date }).forEach {
            assertEquals((6L downTo 0L).map { d -> TODAY.minusDays(d) }, it)
        }
        assertEquals((t.readiness as ReadinessResult.Ready).score, t.readinessWeek.last().score)
        assertEquals(7.125, t.sleepWeek.last().hours!!, 1e-9)
        assertEquals(5760L, t.activityWeek[5].steps)
        assertEquals(8240L, t.activityWeek[6].steps)
        assertEquals(6.1, t.activityWeek[6].distanceKm!!, 1e-9)
        assertNull(t.activityWeek[0].steps)
    }

    @Test fun `suggestions change through the day`() {
        assertEquals(listOf(Suggestion(SuggestionKind.HARD_WORKOUT)), buildToday(full, time(8, 10)).suggestions)
        assertEquals(
            listOf(Suggestion(SuggestionKind.HARD_WORKOUT), Suggestion(SuggestionKind.WALK, 760)),
            buildToday(full, time(21, 30)).suggestions,
        )
    }

    @Test fun `hourly-only steps reach the week series`() {
        val snap = HealthSnapshot(hourlySteps = listOf(HourBucket(at(TODAY, 9), 1000), HourBucket(at(TODAY, 10), 2000)))
        val t = buildToday(snap, time(12))
        assertEquals(3000L, t.activityWeek.last().steps)
        assertEquals(t.activity!!.steps, t.activityWeek.last().steps)
        assertEquals(33, t.movementScore)
    }

    @Test fun `empty snapshot`() {
        val t = buildToday(HealthSnapshot(), time(8))
        assertEquals(ReadinessResult.NoData, t.readiness)
        assertNull(t.movementScore)
        assertTrue(t.suggestions.isEmpty())
        assertTrue(t.readinessWeek.all { it.score == null })
        assertTrue(t.sleepWeek.all { it.hours == null })
        assertTrue(t.activityWeek.all { it.steps == null })
    }


    @Test fun `a past date shows that day without suggestions`() {
        val t = buildToday(full, time(21, 30), date = TODAY.minusDays(1))
        assertEquals(TODAY.minusDays(1), t.date)
        assertFalse(t.isToday)
        assertEquals(5760L, t.activity!!.steps)
        assertTrue(t.suggestions.isEmpty())
        assertEquals(TODAY.minusDays(1), t.trend.last().date)
    }

    @Test fun `previous day scores for deltas`() {
        val t = buildToday(full, time(8))
        assertEquals(64, t.previous.movement)
        assertEquals(t.trend[t.trend.size - 2].recovery, t.previous.readiness)
    }

    @Test fun `goals feed movement`() {
        assertEquals(100, buildToday(full, time(8), goals = Goals(steps = 5000)).movementScore)
    }

    @Test fun `naps are listed and the trend covers 30 days`() {
        val nap = SleepSession(at(TODAY, 14), at(TODAY, 14, 40), emptyList())
        val t = buildToday(full + HealthSnapshot(sleep = listOf(nap)), time(18))
        assertEquals(listOf(nap), t.naps)
        assertEquals(30, t.trend.size)
        assertTrue(TODAY in t.daysWithData)
    }

    @Test fun `water is the total for the selected date`() {
        val snap = HealthSnapshot(waterDaily = mapOf(TODAY to 1.25, TODAY.minusDays(1) to 2.0))
        assertEquals(1.25, buildToday(snap, time(20)).waterLiters, 0.0001)
        assertEquals(2.0, buildToday(snap, time(20), TODAY.minusDays(1)).waterLiters, 0.0001)
        assertEquals(0.0, buildToday(snap, time(20), TODAY.minusDays(2)).waterLiters, 0.0001)
        assertEquals(2.5, buildToday(snap, time(20)).waterGoal, 0.0001)
        assertEquals(3.0, buildToday(snap, time(20), goals = Goals(waterLiters = 3.0)).waterGoal, 0.0001)
    }

    @Test fun `bmi needs both height and weight`() {
        assertNull(buildToday(HealthSnapshot(heightMeters = 1.75), time(20)).bmi)
        assertNull(buildToday(HealthSnapshot(weightKg = 70.0), time(20)).bmiBand)
        val t = buildToday(HealthSnapshot(heightMeters = 1.75, weightKg = 70.0), time(20))
        assertEquals(22.86, t.bmi!!, 0.005)
        assertEquals(BmiBand.HEALTHY, t.bmiBand)
    }
}
