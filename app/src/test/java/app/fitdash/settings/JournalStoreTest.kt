package app.fitdash.settings

import app.fitdash.TODAY
import app.fitdash.at
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JournalStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun `toggle on a past date lands on that date`() {
        val store = JournalStore(tmp.newFile())
        store.toggle(TODAY.minusDays(1), Tag.ALCOHOL)
        assertEquals(setOf(Tag.ALCOHOL), store.flow.value[TODAY.minusDays(1)]?.tags)
        assertNull(store.flow.value[TODAY])
    }

    @Test fun `round trips through the file`() {
        val f = tmp.newFile(); val a = JournalStore(f)
        a.toggle(TODAY, Tag.STRESS); a.add(TODAY, NutrientKind.PROTEIN, Entry(at(TODAY, 9), 25.0, "r1"))
        val b = JournalStore(f)
        assertEquals(a.flow.value, b.flow.value)
    }

    @Test fun `remove drops the entry and empty days vanish`() {
        val s = JournalStore(tmp.newFile()); val t = at(TODAY, 9)
        s.add(TODAY, NutrientKind.CAFFEINE, Entry(t, 95.0, null)); s.remove(TODAY, NutrientKind.CAFFEINE, t)
        assertTrue(s.flow.value.isEmpty())
    }
}
