package app.fitdash.ui

import app.fitdash.health.SourceStatus
import app.fitdash.metrics.Component
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class CopyTest {
    @Test fun `based-on line lists what was used`() {
        assertEquals("Based on resting HR + sleep", basedOn(setOf(Component.SLEEP, Component.RESTING_HR)))
        assertNull(basedOn(Component.entries.toSet()))
    }

    @Test fun `durations and numbers`() {
        assertEquals("7h 12m", formatDuration(Duration.ofMinutes(432)))
        assertEquals("45m", formatDuration(Duration.ofMinutes(45)))
        assertEquals("8,240", formatSteps(8240))
        assertEquals("94", formatNumber(94.0))
        assertEquals("14.5", formatNumber(14.5))
    }

    @Test fun `source lines`() {
        assertEquals("no access", sourceLine(SourceStatus("Weight", false, false, emptySet())))
        assertEquals("no data", sourceLine(SourceStatus("Weight", true, false, emptySet())))
        assertEquals("✓ Fitbit", sourceLine(SourceStatus("Sleep", true, true, setOf("com.fitbit.FitbitMobile"))))
    }
}
