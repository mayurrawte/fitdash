package app.fitdash.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate

enum class Tag(val label: String) {
    LATE_CAFFEINE("Late caffeine"), ALCOHOL("Alcohol"), LATE_MEAL("Late meal"), ILLNESS("Feeling ill"), TRAVEL("Travel"),
    SCREEN_IN_BED("Screen in bed"), STRESS("Stressful day"), LATE_WORKOUT("Late workout"), NAP("Napped"), EARLY_BED("Early night"),
}

enum class NutrientKind { PROTEIN, CAFFEINE }

// Protein grams or caffeine mg.
data class Entry(val at: Instant, val amount: Double, val recordId: String?)

data class DayJournal(
    val tags: Set<Tag> = emptySet(),
    val protein: List<Entry> = emptyList(),
    val caffeine: List<Entry> = emptyList(),
)

class JournalStore(private val file: File) {
    private val _flow = MutableStateFlow(emptyMap<LocalDate, DayJournal>())
    val flow: StateFlow<Map<LocalDate, DayJournal>> = _flow

    init {
        val text = if (file.exists()) file.readText() else ""
        if (text.isNotBlank()) {
            val root = JSONObject(text)
            val days = mutableMapOf<LocalDate, DayJournal>()
            for (key in root.keys()) {
                val day = root.getJSONObject(key)
                val tags = day.optJSONArray("tags") ?: JSONArray()
                val protein = day.optJSONArray("protein") ?: JSONArray()
                val caffeine = day.optJSONArray("caffeine") ?: JSONArray()
                days[LocalDate.parse(key)] = DayJournal(
                    tags = (0 until tags.length()).mapNotNull { i -> runCatching { Tag.valueOf(tags.getString(i)) }.getOrNull() }.toSet(),
                    protein = (0 until protein.length()).map { i ->
                        val e = protein.getJSONObject(i)
                        Entry(Instant.parse(e.getString("at")), e.getDouble("amount"), e.optString("recordId").takeIf { it.isNotEmpty() })
                    },
                    caffeine = (0 until caffeine.length()).map { i ->
                        val e = caffeine.getJSONObject(i)
                        Entry(Instant.parse(e.getString("at")), e.getDouble("amount"), e.optString("recordId").takeIf { it.isNotEmpty() })
                    },
                )
            }
            _flow.value = days
        }
    }

    fun toggle(date: LocalDate, tag: Tag) = update(date) { d ->
        d.copy(tags = if (tag in d.tags) d.tags - tag else d.tags + tag)
    }

    fun add(date: LocalDate, kind: NutrientKind, entry: Entry) = update(date) { d ->
        if (kind == NutrientKind.PROTEIN) d.copy(protein = d.protein + entry) else d.copy(caffeine = d.caffeine + entry)
    }

    fun remove(date: LocalDate, kind: NutrientKind, at: Instant) = update(date) { d ->
        if (kind == NutrientKind.PROTEIN) d.copy(protein = d.protein.filter { it.at != at })
        else d.copy(caffeine = d.caffeine.filter { it.at != at })
    }

    private fun update(date: LocalDate, change: (DayJournal) -> DayJournal) {
        val day = change(_flow.value[date] ?: DayJournal())
        val days = _flow.value.toMutableMap()
        if (day.tags.isEmpty() && day.protein.isEmpty() && day.caffeine.isEmpty()) days.remove(date) else days[date] = day
        val root = JSONObject()
        for ((d, j) in days) {
            val entries = { list: List<Entry> ->
                JSONArray(list.map { JSONObject().put("at", it.at.toString()).put("amount", it.amount).put("recordId", it.recordId ?: "") })
            }
            root.put(
                d.toString(),
                JSONObject().put("tags", JSONArray(j.tags.map { it.name })).put("protein", entries(j.protein)).put("caffeine", entries(j.caffeine)),
            )
        }
        file.writeText(root.toString())
        _flow.value = days
    }

    companion object {
        @Volatile private var instance: JournalStore? = null

        fun get(context: Context): JournalStore =
            instance ?: synchronized(this) {
                instance ?: JournalStore(File(context.filesDir, "journal.json")).also { instance = it }
            }
    }
}
