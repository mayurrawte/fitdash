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
) {
    fun entries(kind: NutrientKind) = if (kind == NutrientKind.PROTEIN) protein else caffeine

    fun withEntries(kind: NutrientKind, list: List<Entry>) = if (kind == NutrientKind.PROTEIN) copy(protein = list) else copy(caffeine = list)
}

class JournalStore(private val file: File) {
    private val _flow = MutableStateFlow(emptyMap<LocalDate, DayJournal>())
    val flow: StateFlow<Map<LocalDate, DayJournal>> = _flow

    init {
        val text = if (file.exists()) file.readText() else ""
        if (text.isNotBlank()) {
            val root = JSONObject(text)
            val days = mutableMapOf<LocalDate, DayJournal>()
            val entries = { arr: JSONArray ->
                (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    Entry(Instant.parse(e.getString("at")), e.getDouble("amount"), e.optString("recordId").takeIf { it.isNotEmpty() })
                }
            }
            for (key in root.keys()) {
                val day = root.getJSONObject(key)
                val tags = day.optJSONArray("tags") ?: JSONArray()
                var j = DayJournal(tags = (0 until tags.length()).mapNotNull { i -> runCatching { Tag.valueOf(tags.getString(i)) }.getOrNull() }.toSet())
                for (kind in NutrientKind.entries) j = j.withEntries(kind, entries(day.optJSONArray(kind.name.lowercase()) ?: JSONArray()))
                days[LocalDate.parse(key)] = j
            }
            _flow.value = days
        }
    }

    fun toggle(date: LocalDate, tag: Tag) = update(date) { d ->
        d.copy(tags = if (tag in d.tags) d.tags - tag else d.tags + tag)
    }

    fun add(date: LocalDate, kind: NutrientKind, entry: Entry) = update(date) { d -> d.withEntries(kind, d.entries(kind) + entry) }

    fun remove(date: LocalDate, kind: NutrientKind, at: Instant) = update(date) { d -> d.withEntries(kind, d.entries(kind).filter { it.at != at }) }

    fun setRecordId(date: LocalDate, kind: NutrientKind, at: Instant, recordId: String) = update(date) { d ->
        d.withEntries(kind, d.entries(kind).map { if (it.at == at) it.copy(recordId = recordId) else it })
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
            val json = JSONObject().put("tags", JSONArray(j.tags.map { it.name }))
            for (kind in NutrientKind.entries) json.put(kind.name.lowercase(), entries(j.entries(kind)))
            root.put(d.toString(), json)
        }
        val tmp = File.createTempFile("journal", ".tmp", file.parentFile)
        tmp.writeText(root.toString())
        tmp.renameTo(file)
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
