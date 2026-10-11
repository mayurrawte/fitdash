package app.fitdash.metrics

import app.fitdash.IST
import app.fitdash.TODAY
import app.fitdash.at
import app.fitdash.night
import app.fitdash.health.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class SleepTest {
    @Test fun `nightEnding picks the longest session, not a nap`() {
        val main = night(TODAY, 7.0)
        val nap = SleepSession(at(TODAY, 14), at(TODAY, 14, 40), emptyList())
        assertEquals(main, nightEnding(TODAY, listOf(nap, main), IST))
    }

    @Test fun `a night that crosses midnight belongs to the morning it ends`() {
        val n = night(TODAY, 7.0) // 23:00 on 10 Oct to 06:00 on 11 Oct
        assertEquals(n, nightEnding(TODAY, listOf(n), IST))
        assertNull(nightEnding(TODAY.minusDays(1), listOf(n), IST))
    }

    @Test fun `asleep time excludes awake stages`() {
        assertEquals(Duration.ofMinutes(456), asleepDuration(night(TODAY, 8.0, stages = true)))
        assertEquals(Duration.ofMinutes(432), asleepDuration(night(TODAY, 7.2)))
    }

    @Test fun `deep plus REM share needs stages`() {
        assertEquals(0.45 / 0.95, deepRemShare(night(TODAY, 8.0, stages = true))!!, 1e-6)
        assertNull(deepRemShare(night(TODAY, 8.0)))
    }

    @Test fun `sleep score without stages is hours over target`() {
        assertEquals(90, sleepScore(night(TODAY, 7.2), null))
        assertEquals(100, sleepScore(night(TODAY, 9.5), null))
    }

    @Test fun `stage share above baseline adds points, capped at 10`() {
        val n = night(TODAY, 6.0, stages = true) // 5.7 h asleep -> 71.25
        assertEquals(71, sleepScore(n, 0.45 / 0.95))
        assertEquals(76, sleepScore(n, 0.45 / 0.95 - 0.05))
        assertEquals(61, sleepScore(n, 0.45 / 0.95 + 0.20))
    }

    @Test fun `summary reports stage durations`() {
        val sessions = (0..10).map { night(TODAY.minusDays(it.toLong()), 8.0, stages = true) }
        val s = summarizeSleep(TODAY, sessions, IST)!!
        assertTrue(s.hasStages)
        assertEquals(Duration.ofMinutes(96), s.deep)
        assertEquals(Duration.ofMinutes(120), s.rem)
        assertEquals(Duration.ofMinutes(240), s.light)
        assertEquals(Duration.ofMinutes(24), s.awake)
        assertEquals(95, s.score)
    }

    @Test fun `summary without stages`() {
        val s = summarizeSleep(TODAY, listOf(night(TODAY, 7.2)), IST)!!
        assertFalse(s.hasStages)
        assertEquals(90, s.score)
        assertNull(summarizeSleep(TODAY.minusDays(3), listOf(night(TODAY, 7.2)), IST))
    }
}
