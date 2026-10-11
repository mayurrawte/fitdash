package app.fitdash.ui

import androidx.compose.ui.graphics.Color
import app.fitdash.ui.theme.breathMillis
import app.fitdash.ui.theme.scoreColor
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreColorTest {
    @Test fun `traffic light stops`() {
        assertEquals(Color(229, 72, 77), scoreColor(0))
        assertEquals(Color(245, 176, 65), scoreColor(50))
        assertEquals(Color(61, 220, 151), scoreColor(100))
    }

    @Test fun `interpolates and clamps`() {
        assertEquals(Color(237, 124, 71), scoreColor(25))
        assertEquals(scoreColor(0), scoreColor(-5))
        assertEquals(scoreColor(100), scoreColor(130))
    }

    @Test fun `breathing slows when the score is low`() {
        assertEquals(7000, breathMillis(15))
        assertEquals(7000, breathMillis(5))
        assertEquals(4000, breathMillis(98))
    }
}
