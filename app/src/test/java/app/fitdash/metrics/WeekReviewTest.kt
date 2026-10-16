package app.fitdash.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeekReviewTest {
    private val end = LocalDate.of(2026, 10, 11)

    // 14 days ending on `end`: the first seven at `before`, the last seven at `after`.
    private fun twoWeeks(before: Double?, after: Double?) =
        (13 downTo 0).map { DayValue(end.minusDays(it.toLong()), if (it >= 7) before else after) }

    @Test fun `compares this week with the one before`() {
        val r = weekReview(mapOf(MetricKind.STEPS to twoWeeks(8000.0, 9000.0)), end)!!
        assertEquals(WeekRow(MetricKind.STEPS, 9000.0, 8000.0), r.rows.single())
        assertEquals("You walked 13% more than the week before.", r.headline)
    }

    @Test fun `headline picks the biggest change`() {
        val r = weekReview(
            mapOf(MetricKind.STEPS to twoWeeks(8000.0, 8200.0), MetricKind.SLEEP_HOURS to twoWeeks(7.5, 6.5)),
            end,
        )!!
        assertEquals("You slept 60 minutes less a night than the week before.", r.headline)
    }

    @Test fun `small changes read as a steady week`() {
        val r = weekReview(mapOf(MetricKind.RECOVERY to twoWeeks(70.0, 71.0)), end)!!
        assertEquals("A steady week: everything within 5% of the week before.", r.headline)
    }

    @Test fun `no previous week means no comparison`() {
        val r = weekReview(mapOf(MetricKind.STEPS to twoWeeks(null, 9000.0)), end)!!
        assertNull(r.rows.single().lastWeek)
        assertNull(r.headline)
    }

    @Test fun `too few days gives no review`() {
        val sparse = (13 downTo 0).map { DayValue(end.minusDays(it.toLong()), if (it < 2) 9000.0 else null) }
        assertNull(weekReview(mapOf(MetricKind.STEPS to sparse), end))
    }
}
