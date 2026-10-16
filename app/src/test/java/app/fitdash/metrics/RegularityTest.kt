package app.fitdash.metrics

import app.fitdash.IST
import app.fitdash.TODAY
import app.fitdash.at
import app.fitdash.health.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RegularityTest {
    private fun night(date: LocalDate, startHour: Int, endHour: Int, startMinute: Int = 0): SleepSession {
        val start = if (startHour >= 18) at(date.minusDays(1), startHour, startMinute) else at(date, startHour, startMinute)
        return SleepSession(start, at(date, endHour), emptyList())
    }

    private fun nights(days: Int, build: (Int, LocalDate) -> SleepSession) = (days - 1 downTo 0).map { i -> build(i, TODAY.minusDays(i.toLong())) }

    @Test fun `identical nights give 100`() {
        val r = regularity(nights(14) { _, d -> night(d, 23, 7) }, TODAY, IST)!!
        assertEquals(100, r.value.sri)
        assertEquals(14, r.value.nightsUsed)
        assertEquals(13, r.value.pairsUsed)
    }

    @Test fun `alternating nights give 33`() {
        // 22:00–06:00 one day, 02:00–10:00 the next: states differ for 8 of 24 hours, matches 16/24, SRI = 200*16/24-100 = 33
        val r = regularity(nights(14) { i, d -> if (i % 2 == 0) night(d, 22, 6) else night(d, 2, 10) }, TODAY, IST)!!
        // The last day's unrecorded tail is excluded from pairing, which nudges the share up by under one point.
        assert(r.value.sri in 33..34) { "sri ${r.value.sri}" }
    }

    @Test fun `under five valid pairs is null and progress counts pairs`() {
        val five = nights(5) { _, d -> night(d, 23, 7) }
        assertNull(regularity(five, TODAY, IST))
        assertEquals(4, regularityProgress(five, TODAY, IST))
        val six = nights(6) { _, d -> night(d, 23, 7) }
        assertEquals(100, regularity(six, TODAY, IST)!!.value.sri)
    }

    @Test fun `a missing night is excluded not counted as awake`() {
        val full = nights(14) { _, d -> night(d, 23, 7) }
        val gap = full.filterNot { it.end == at(TODAY.minusDays(6), 7) }
        val r = regularity(gap, TODAY, IST)!!
        assertEquals(100, r.value.sri)
        assertEquals(13, r.value.nightsUsed)
        assertEquals(11, r.value.pairsUsed)
    }

    @Test fun `naps count as asleep`() {
        val base = nights(14) { _, d -> night(d, 23, 7) }
        val nap = SleepSession(at(TODAY.minusDays(3), 14), at(TODAY.minusDays(3), 15), emptyList())
        val r = regularity(base + nap, TODAY, IST)!!
        // one hour mismatched on two pairs out of 13 × 24 h: 2/312 → 200 × (1 − 2/312) − 100 ≈ 98.7 → 99
        assertEquals(99, r.value.sri)
    }

    @Test fun `bedtime spread wraps midnight`() {
        val d = TODAY
        val times = listOf(at(d.minusDays(1), 23, 30), at(d, 0, 30), at(d.minusDays(1), 23, 0))
        // minutes from 18:00: 330, 390, 300 → mean 340, population SD = sqrt((100+2500+1600)/3) = 37.4 → 37
        assertEquals(37, clockSpreadMinutes(times, IST))
        assertNull(clockSpreadMinutes(times.take(2), IST))
    }

    @Test fun `a DST change inside the window does not shift pairing`() {
        val london: ZoneId = ZoneId.of("Europe/London")
        val end = LocalDate.of(2026, 11, 1) // clocks went back on 25 Oct 2026
        fun ln(date: LocalDate) = SleepSession(date.minusDays(1).atTime(23, 0).atZone(london).toInstant(), date.atTime(7, 0).atZone(london).toInstant(), emptyList())
        val sessions = (13 downTo 0).map { ln(end.minusDays(it.toLong())) }
        val r = regularity(sessions, end, london)!!
        // the night across the change is one hour longer, so one pair mismatches by an hour each side: still rounds above 98
        assert(r.value.sri >= 98) { "sri ${r.value.sri}" }
    }

    @Test fun `word bands`() {
        assertEquals(0, regularityWordBand(80))
        assertEquals(1, regularityWordBand(79))
        assertEquals(1, regularityWordBand(70))
        assertEquals(2, regularityWordBand(69))
        assertEquals(3, regularityWordBand(59))
    }
}
