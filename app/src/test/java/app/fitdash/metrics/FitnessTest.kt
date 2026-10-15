package app.fitdash.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class FitnessTest {
    @Test fun `PA-R from workouts and steps follows the table`() {
        assertEquals(0, paRating(0.0, 2000.0))
        assertEquals(1, paRating(0.0, 3000.0))
        assertEquals(1, paRating(0.0, 6999.0))
        assertEquals(2, paRating(0.0, 7000.0))
        assertEquals(2, paRating(30.0, 4000.0))
        assertEquals(3, paRating(60.0, 4000.0))
        assertEquals(3, paRating(89.9, 4000.0))
        assertEquals(4, paRating(90.0, 4000.0))
        assertEquals(4, paRating(30.0, 10000.0))
        assertEquals(5, paRating(150.0, 4000.0))
        assertEquals(6, paRating(240.0, 4000.0))
        assertEquals(7, paRating(420.0, 4000.0))
        assertEquals(7, paRating(900.0, 0.0))
    }

    @Test fun `Jackson 1990 worked example`() {
        // 56.363 + 1.921*3 - 0.381*40 - 0.754*25 + 10.987 = 39.023
        assertEquals(39.02, jacksonVo2(40, Gender.MALE, 25.0, 3), 0.01)
        assertEquals(28.04, jacksonVo2(40, Gender.FEMALE, 25.0, 3), 0.01)
    }

    @Test fun `fitness age is a fixed point at the reference inputs`() {
        for (age in 18..90) {
            for (g in Gender.entries) {
                assertEquals(age, fitnessAge(age, g, 25.0, 3).value.years)
            }
        }
    }

    @Test fun `fitter inputs give a younger fitness age and the sheet lists them`() {
        val r = fitnessAge(40, Gender.MALE, 22.0, 6)
        assertTrue(r.value.years < 40)
        assertEquals(40, r.value.realAge)
        assertEquals(listOf("Age" to "40", "Gender" to "Male", "BMI" to "22.0", "Activity rating" to "6 of 7"), r.explanation.inputs)
        assertEquals("Jackson et al. 1990", r.explanation.source.title)
    }

    @Test fun `fitness age clamps to 18 and 90`() {
        assertEquals(18, fitnessAge(20, Gender.MALE, 15.0, 7).value.years)
        assertEquals(90, fitnessAge(85, Gender.FEMALE, 45.0, 0).value.years)
    }
}
