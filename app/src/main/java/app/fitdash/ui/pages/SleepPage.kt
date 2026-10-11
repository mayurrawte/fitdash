package app.fitdash.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.fitdash.metrics.Status
import app.fitdash.metrics.TYPICAL_AWAKE
import app.fitdash.metrics.TYPICAL_DEEP
import app.fitdash.metrics.TYPICAL_LIGHT
import app.fitdash.metrics.TYPICAL_REM
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.VitalReading
import app.fitdash.metrics.asleepDuration
import app.fitdash.metrics.rangeStatus
import app.fitdash.metrics.rangeWord
import app.fitdash.metrics.scoreStatus
import app.fitdash.metrics.shareStatus
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.EmptyState
import app.fitdash.ui.components.EmptyTrack
import app.fitdash.ui.components.Heading
import app.fitdash.ui.components.Hypnogram
import app.fitdash.ui.components.ListRow
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.NumberTile
import app.fitdash.ui.components.RangeBar
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.SectionLabel
import app.fitdash.ui.components.SemiGauge
import app.fitdash.ui.components.SpanBar
import app.fitdash.ui.components.StatTile
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.components.TypicalShareBar
import app.fitdash.ui.dayLetter
import app.fitdash.ui.formatDuration
import app.fitdash.ui.formatNumber
import app.fitdash.ui.formatTime12
import app.fitdash.ui.rangeNote
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import java.time.Duration
import java.time.ZoneId
import kotlin.math.roundToInt

private fun hoursMins(d: Duration): List<Pair<String, String>> =
    if (d.toHours() == 0L) listOf((d.toMinutes() % 60).toString() to "mins")
    else listOf(d.toHours().toString() to "hrs", (d.toMinutes() % 60).toString() to "mins")

@Composable
fun SleepPage(data: TodayData, chrome: PageChrome) {
    val s = data.sleep
    val zone = ZoneId.systemDefault()
    PageColumn(chrome, accent = Palette.Rem) {
        if (s == null) {
            item(key = "empty") { EmptyState(if (data.isToday) "No sleep last night" else "No sleep that night", "Wear your Fitbit to bed and sync it. Your night will show up here.") }
        } else {
            item(key = "gauge") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    SemiGauge(s.score, "Sleep", scoreStatus(s.score))
                    SpanBar(formatTime12(s.start, zone), formatTime12(s.end, zone), Modifier.padding(horizontal = 22.dp, vertical = Space.s))
                }
            }
            item(key = "last-night") { Heading(if (data.isToday) "Last night" else "That night") }
            item(key = "sleep-tiles") {
                val hrCeiling = data.heart?.restingHrRange?.endInclusive
                TileGrid(
                    listOf<@Composable (Modifier) -> Unit>(
                        { m -> NumberTile("Total sleep", NavIcons.moonSmall, hoursMins(s.asleep), m, scoreStatus(s.score)) },
                        { m -> NumberTile("Time in bed", NavIcons.clock, hoursMins(s.timeInBed), m) },
                        { m ->
                            NumberTile(
                                "Restorative", NavIcons.moonSmall,
                                listOf((s.restorative?.let { (it * 100).roundToInt().toString() } ?: "—") to if (s.restorative != null) "%" else ""),
                                m, s.restorative?.let { shareStatus(it, 0.33..0.48) },
                            )
                        },
                        { m ->
                            NumberTile(
                                "Overnight HR", NavIcons.heart,
                                listOf((s.overnightHr?.let { formatNumber(it) } ?: "—") to if (s.overnightHr != null) "bpm" else ""),
                                m, if (s.overnightHr != null && hrCeiling != null) (if (s.overnightHr <= hrCeiling) Status.GOOD else Status.FAIR) else null,
                            )
                        },
                    ),
                )
            }
        }
        data.naps.forEachIndexed { i, nap ->
            item(key = "nap-$i") {
                ListRow(NavIcons.bed, "Nap", "${formatDuration(asleepDuration(nap))} · ${formatTime12(nap.start, zone)} – ${formatTime12(nap.end, zone)}")
            }
        }
        if (s != null) {
            if (s.hasStages) {
                revealItem("stages", 0) {
                    SectionCard("STAGES") {
                        Hypnogram("sleep-hypnogram", s.stages)
                        Row(Modifier.fillMaxWidth().padding(top = Space.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime12(s.stages.minOf { it.start }, zone).uppercase(), style = Type.tiny, color = Palette.Muted)
                            Text(formatTime12(s.stages.maxOf { it.end }, zone).uppercase(), style = Type.tiny, color = Palette.Muted)
                        }
                    }
                }
                val night = (s.asleep + s.awake).seconds.toFloat().coerceAtLeast(1f)
                revealItem("architecture", 1) {
                    SectionCard("SLEEP ARCHITECTURE") {
                        listOf(
                            Triple("Deep", s.deep, TYPICAL_DEEP),
                            Triple("Light", s.light, TYPICAL_LIGHT),
                            Triple("REM", s.rem, TYPICAL_REM),
                            Triple("Awake", s.awake, TYPICAL_AWAKE),
                        ).forEach { (name, d, typical) ->
                            val share = d.seconds / night
                            Column(Modifier.padding(vertical = Space.s)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, style = Type.small, color = Palette.Text)
                                    Text("${formatDuration(d)} · ${(share * 100).roundToInt()}%".uppercase(), style = Type.tiny, color = Palette.Muted)
                                }
                                TypicalShareBar(share, typical.start.toFloat()..typical.endInclusive.toFloat(), Modifier.padding(top = Space.xs), key = "arch-$name-$share")
                            }
                        }
                        Text("DASHED BRACKET: TYPICAL RANGE", style = Type.tiny, color = Palette.Faint, modifier = Modifier.padding(top = Space.xs))
                    }
                }
            } else {
                revealItem("duration", 0) {
                    SectionCard("DURATION", trailing = "goal ${data.goals.sleepHours.roundToInt()}h") {
                        LinearProgressIndicator(
                            progress = { (s.asleep.toMinutes() / (data.goals.sleepHours * 60)).toFloat().coerceAtMost(1f) },
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
                            StatTile(
                                v.label, formatNumber(v.value), v.unit, rangeNote(v.value, v.range, v.good), m,
                                status = v.range?.let { rangeWord(rangeStatus(v.value, it, v.good)) },
                            ) { v.range?.let { RangeBar("vital-${v.label}", v.value, it, v.good) } ?: EmptyTrack() }
                        }
                    },
                )
            }
        }
        if (data.sleepWeek.any { it.hours != null }) {
            val known = data.sleepWeek.mapNotNull { it.hours }
            revealItem("sleep-week", 2) {
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
