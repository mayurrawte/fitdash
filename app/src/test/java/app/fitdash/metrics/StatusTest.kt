package app.fitdash.metrics

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusTest {
    @Test fun `score status bands`() {
        assertEquals(Status.OPTIMAL, scoreStatus(85))
        assertEquals(Status.GOOD, scoreStatus(84))
        assertEquals(Status.GOOD, scoreStatus(70))
        assertEquals(Status.FAIR, scoreStatus(50))
        assertEquals(Status.ATTENTION, scoreStatus(49))
    }

    @Test fun `range and share words`() {
        assertEquals(Status.OPTIMAL, rangeWord(RangeStatus.GOOD_SIDE))
        assertEquals(Status.GOOD, rangeWord(RangeStatus.IN))
        assertEquals(Status.ATTENTION, rangeWord(RangeStatus.BAD_SIDE))
        assertEquals(Status.OPTIMAL, shareStatus(0.15, TYPICAL_DEEP))
        assertEquals(Status.FAIR, shareStatus(0.08, TYPICAL_DEEP))
    }
}
