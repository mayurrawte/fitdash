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
import app.fitdash.health.Sample
import org.junit.Assert.assertEquals
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

    @Test fun `empty snapshot`() {
        val t = buildToday(HealthSnapshot(), time(8))
        assertEquals(ReadinessResult.NoData, t.readiness)
        assertNull(t.movementScore)
        assertTrue(t.suggestions.isEmpty())
        assertTrue(t.readinessWeek.all { it.score == null })
        assertTrue(t.sleepWeek.all { it.hours == null })
        assertTrue(t.activityWeek.all { it.steps == null })
    }
}
