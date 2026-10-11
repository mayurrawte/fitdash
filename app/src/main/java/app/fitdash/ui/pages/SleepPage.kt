package app.fitdash.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
import app.fitdash.metrics.VitalReading
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.EmptyState
import app.fitdash.ui.components.EmptyTrack
import app.fitdash.ui.components.Hypnogram
import app.fitdash.ui.components.MiniRing
import app.fitdash.ui.components.RangeBar
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.SectionLabel
import app.fitdash.ui.components.StatTile
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.dayLetter
import app.fitdash.ui.formatClock
import app.fitdash.ui.formatDuration
import app.fitdash.ui.formatNumber
import app.fitdash.ui.rangeNote
import app.fitdash.ui.sleepWord
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import java.time.Duration
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
fun SleepPage(data: TodayData, chrome: PageChrome) {
    val s = data.sleep
    val zone = ZoneId.systemDefault()
    val headerColor = s?.let { scoreColor(it.score) }
    PageColumn(chrome, accent = Palette.Rem) {
        if (s == null) {
            item(key = "empty") { EmptyState("No sleep last night", "Wear your Fitbit to bed and sync it. Your night will show up here.") }
        } else {
            pageHeader("LAST NIGHT", formatDuration(s.asleep), "Sleep score ${s.score} · ${sleepWord(s.score)}", headerColor)
            if (s.hasStages) {
                revealItem("stages", 0) {
                    SectionCard("STAGES") {
                        Hypnogram("sleep-hypnogram", s.stages)
                        Row(Modifier.fillMaxWidth().padding(top = Space.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatClock(s.stages.minOf { it.start }, zone), style = Type.tiny, color = Palette.Faint)
                            Text(formatClock(s.stages.maxOf { it.end }, zone), style = Type.tiny, color = Palette.Faint)
                        }
                    }
                }
                val night = (s.asleep + s.awake).seconds.toFloat().coerceAtLeast(1f)
                fun share(d: Duration) = d.seconds / night
                item(key = "stage-tiles") {
                    TileGrid(
                        listOf<@Composable (Modifier) -> Unit>(
                            { m -> StatTile("Deep", formatDuration(s.deep), null, "${(share(s.deep) * 100).roundToInt()}% of the night", m) { MiniRing("stage-deep", share(s.deep), Palette.Deep) } },
                            { m -> StatTile("REM", formatDuration(s.rem), null, "${(share(s.rem) * 100).roundToInt()}% of the night", m) { MiniRing("stage-rem", share(s.rem), Palette.Rem) } },
                            { m -> StatTile("Light", formatDuration(s.light), null, "${(share(s.light) * 100).roundToInt()}% of the night", m) { MiniRing("stage-light", share(s.light), Palette.Light) } },
                            { m -> StatTile("Awake", formatDuration(s.awake), null, "${(share(s.awake) * 100).roundToInt()}% of the night", m) { MiniRing("stage-awake", share(s.awake), Palette.Awake) } },
                        ),
                    )
                }
            } else {
                revealItem("duration", 0) {
                    SectionCard("DURATION", trailing = "goal ${Tuning.SLEEP_TARGET_HOURS.roundToInt()}h") {
                        LinearProgressIndicator(
                            progress = { (s.asleep.toMinutes() / (Tuning.SLEEP_TARGET_HOURS * 60)).toFloat().coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = scoreColor(s.score),
                            trackColor = Palette.Divider,
                        )
                    }
                }
            }
        }
        if (data.vitals.isNotEmpty()) {
            item(key = "vitals-label") { SectionLabel("OVERNIGHT VITALS") }
            item(key = "vitals") {
                TileGrid(
                    data.vitals.map<VitalReading, @Composable (Modifier) -> Unit> { v ->
                        { m ->
                            StatTile(v.label, formatNumber(v.value), v.unit, rangeNote(v.value, v.range, v.good), m) {
                                v.range?.let { RangeBar("vital-${v.label}", v.value, it, v.good) } ?: EmptyTrack()
                            }
                        }
                    },
                )
            }
        }
        if (data.sleepWeek.any { it.hours != null }) {
            val known = data.sleepWeek.mapNotNull { it.hours }
            revealItem("sleep-week", 1) {
                SectionCard("SLEEP · 7 NIGHTS", trailing = "avg ${formatDuration(Duration.ofMinutes((known.average() * 60).roundToInt().toLong()))}") {
                    val divider = Palette.Divider
                    val nightColors = data.sleepWeek.map { d -> d.score?.let { scoreColor(it) } ?: divider }
                    Bars(
                        "sleep-week",
                        data.sleepWeek.map { (it.hours ?: 0.0).toFloat() },
                        { i -> nightColors[i] },
                        labels = data.sleepWeek.map { dayLetter(it.date) },
                    )
                }
            }
        }
    }
}
