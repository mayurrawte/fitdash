package app.fitdash.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MorningAlertTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 10, 26)
    private fun at(h: Int, m: Int = 0) = day.atTime(h, m).atZone(zone)
    private fun heart(rhr: Double?, hrv: Double?) = HeartSummary(hrv, 47.0, rhr, 66.0, emptyList(), emptyList(), 40.0..55.0, 62.0..70.0)

    @Test fun `resting heart rate above its range is returned`() {
        val alerts = morningAlert(emptyList(), heart(76.0, 48.0))
        assertEquals(listOf("Resting HR"), alerts.map { it.label })
        assertEquals(76.0, alerts[0].value, 0.0)
        assertEquals(62.0..70.0, alerts[0].range)
    }

    @Test fun `HRV below its range is returned`() {
        assertEquals(listOf("HRV"), morningAlert(emptyList(), heart(65.0, 37.0)).map { it.label })
    }

    @Test fun `values inside their range are not returned`() {
        assertTrue(morningAlert(emptyList(), heart(65.0, 48.0)).isEmpty())
    }

    @Test fun `the good side of a range is not returned`() {
        assertTrue(morningAlert(emptyList(), heart(58.0, 60.0)).isEmpty())
    }

    @Test fun `vitals outside their range are returned`() {
        val spo2 = VitalReading("SpO₂", "%", 91.0, 96.0, 94.0..99.0, Good.HIGHER, MetricKind.SPO2)
        val steady = VitalReading("Respiratory rate", "br/min", 15.0, 15.0, 13.0..17.0, Good.MIDDLE, MetricKind.RESPIRATORY)
        val noRange = VitalReading("Skin temperature", "°C", 1.0, null, null, Good.MIDDLE, MetricKind.SKIN_TEMP)
        assertEquals(listOf(spo2), morningAlert(listOf(spo2, steady, noRange), null))
    }

    @Test fun `due between 6 AM and 11 AM`() {
        assertFalse(morningAlertDue(at(5, 59), null))
        assertTrue(morningAlertDue(at(6), null))
        assertTrue(morningAlertDue(at(8), null))
        assertFalse(morningAlertDue(at(11), null))
    }

    @Test fun `not due again once sent today`() {
        assertFalse(morningAlertDue(at(9), at(7).toInstant()))
        assertTrue(morningAlertDue(at(9), at(7).minusDays(1).toInstant()))
    }
}
