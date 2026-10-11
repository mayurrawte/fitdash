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

    @Test fun `hero switches at exactly 16_00`() {
        assertEquals(HeroMode.READINESS, buildToday(full, time(15, 59)).hero)
        assertEquals(HeroMode.ACTIVITY, buildToday(full, time(16, 0)).hero)
    }

    @Test fun `morning order and yesterday's activity`() {
        val t = buildToday(full, time(8, 10))
        assertEquals(
            listOf(CardKind.SLEEP, CardKind.HEART, CardKind.VITALS, CardKind.ACTIVITY, CardKind.WEEK, CardKind.GENERIC, CardKind.SOURCES),
            t.cards,
        )
        assertEquals(TODAY.minusDays(1), t.activity!!.date)
    }

    @Test fun `evening order and today's activity`() {
        val t = buildToday(full, time(21, 30))
        assertEquals(
            listOf(CardKind.ACTIVITY, CardKind.READINESS_COMPACT, CardKind.SLEEP, CardKind.HEART, CardKind.VITALS, CardKind.WEEK, CardKind.GENERIC, CardKind.SOURCES),
            t.cards,
        )
        assertEquals(TODAY, t.activity!!.date)
    }

    @Test fun `empty snapshot shows only data sources`() {
        val t = buildToday(HealthSnapshot(), time(8))
        assertEquals(listOf(CardKind.SOURCES), t.cards)
        assertEquals(ReadinessResult.NoData, t.readiness)
        assertEquals(7, t.week.size)
        assertTrue(t.week.all { it.score == null })
    }

    @Test fun `week ends today with today's score`() {
        val t = buildToday(full, time(8))
        assertEquals(TODAY, t.week.last().date)
        assertEquals((t.readiness as ReadinessResult.Ready).score, t.week.last().score)
    }
}
