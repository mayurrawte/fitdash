package app.fitdash.ui

import app.fitdash.IST
import app.fitdash.TODAY
import app.fitdash.at
import app.fitdash.day
import app.fitdash.history
import app.fitdash.plus
import app.fitdash.health.GenericSeries
import app.fitdash.health.HealthSnapshot
import app.fitdash.health.Sample
import app.fitdash.metrics.buildToday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSummaryTest {
    private val now = TODAY.atTime(8, 10).atZone(IST)
    private val snap = history(TODAY, 10) + day(TODAY, hrv = 40.0, rhr = 60.0, sleepHours = 6.4) +
        HealthSnapshot(generic = listOf(GenericSeries("Weight", "kg", listOf(Sample(at(TODAY, 8), 72.4)))))
    private val text = aiSummary(snap, buildToday(snap, now), now)

    @Test fun `header and today line`() {
        assertTrue(text.startsWith("My health data from fitdash (Health Connect), generated 2026-10-11 08:10.\n"))
        assertTrue(
            text.contains(
                "Today: recovery 73 (Primed), sleep 6h 24m (score 80), HRV 40 ms (baseline 40), " +
                    "resting HR 60 bpm (baseline 60), steps so far no data.",
            ),
        )
    }

    @Test fun `AI rows keep empty days`() {
        assertEquals(30, text.lines().count { it.startsWith("2026-") })
        assertTrue(text.contains("\n2026-09-12, , , , , \n"))
        assertTrue(text.contains("\n2026-10-10, 79, 8.0, 40, 60, \n"))
        assertTrue(text.contains("\n2026-10-11, 73, 6.4, 40, 60, \n"))
    }

    @Test fun `other data line`() {
        assertTrue(text.endsWith("Other data (latest): Weight 72.4 kg"))
    }
}
