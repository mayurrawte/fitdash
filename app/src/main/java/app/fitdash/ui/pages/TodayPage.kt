package app.fitdash.ui.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.metrics.Good
import app.fitdash.metrics.MetricKind
import app.fitdash.metrics.ReadinessResult
import app.fitdash.metrics.SuggestionKind
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
import app.fitdash.metrics.asleepDuration
import app.fitdash.metrics.rangeStatus
import app.fitdash.metrics.rangeWord
import app.fitdash.ui.bandAdvice
import app.fitdash.ui.bandWord
import app.fitdash.ui.basedOn
import app.fitdash.ui.components.Chart
import app.fitdash.ui.components.CountUpText
import app.fitdash.ui.components.EmptyTrack
import app.fitdash.ui.components.Heading
import app.fitdash.ui.components.ListRow
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.Orb
import app.fitdash.ui.components.RangeBar
import app.fitdash.ui.components.SectionLabel
import app.fitdash.ui.components.StatTile
import app.fitdash.ui.components.cardSurface
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.components.WeekStrip
import app.fitdash.ui.formatDuration
import app.fitdash.ui.formatLiters
import app.fitdash.ui.formatSteps
import app.fitdash.ui.formatTime12
import app.fitdash.ui.rangeNote
import app.fitdash.ui.suggestionText
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

private const val UNDO_MILLIS = 5000L

private fun suggestionIcon(kind: SuggestionKind) = when (kind) {
    SuggestionKind.HARD_WORKOUT -> NavIcons.bolt
    SuggestionKind.MODERATE -> NavIcons.walk
    SuggestionKind.REST -> NavIcons.moonSmall
    SuggestionKind.EARLIER_BED -> NavIcons.moonSmall
    SuggestionKind.EASY_ON_STIMULANTS -> NavIcons.cup
    SuggestionKind.WALK -> NavIcons.walk
}

@Composable
fun TodayPage(
    data: TodayData,
    chrome: PageChrome,
    pageOffset: () -> Float,
    onOpen: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onShareDay: () -> Unit,
    onAskAi: () -> Unit,
    onAddWater: (Int) -> Unit,
    onUndoWater: () -> Unit,
) {
    val r = data.readiness
    val score = (r as? ReadinessResult.Ready)?.score
    val zone = ZoneId.systemDefault()
    val openRecovery = metricOpener(MetricRef.Kind(MetricKind.RECOVERY))
    val openSleep = metricOpener(MetricRef.Kind(MetricKind.SLEEP_HOURS))
    val openSteps = metricOpener(MetricRef.Kind(MetricKind.STEPS))
    val openHrv = metricOpener(MetricRef.Kind(MetricKind.HRV))
    val openRhr = metricOpener(MetricRef.Kind(MetricKind.RESTING_HR))
    PageColumn(chrome, accent = score?.let { scoreColor(it) } ?: Palette.Calibrating) {
        item(key = "strip") {
            val monday = data.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            WeekStrip(
                (0L..6L).map { monday.plusDays(it) },
                data.date,
                data.daysWithData,
                onSelectDate,
                enabled = { it <= chrome.today && it >= chrome.today.minusDays(DAYS_BACK) },
            )
        }
        item(key = "orb") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val offset = pageOffset()
                        translationX = offset * size.width * 0.15f
                        alpha = 1f - abs(offset).coerceAtMost(1f)
                    }
                    .semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Orb(score, Modifier.padding(top = Space.s), intro = true) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (score != null) CountUpText("orb-score", score.toLong(), Type.hero) else Text("—", style = Type.hero)
                        Text(if (r is ReadinessResult.Calibrating) "CALIBRATING" else if (r is ReadinessResult.NoData) "NO DATA" else "RECOVERY ›", style = Type.label)
                    }
                }
                val (word, advice) = when (r) {
                    is ReadinessResult.Ready -> bandWord(r.band) to bandAdvice(r.band)
                    is ReadinessResult.Calibrating -> "Night ${r.nights} of ${Tuning.MIN_NIGHTS}" to
                        if (data.isToday) "Wear your Fitbit to bed. Recovery appears after ${Tuning.MIN_NIGHTS} nights of data." else "Not enough earlier data for this day"
                    ReadinessResult.NoData -> "No data for last night" to "Sync your Fitbit, then pull down to refresh."
                }
                Text(word.uppercase(), style = Type.label, color = score?.let { scoreColor(it) } ?: Palette.Muted, modifier = Modifier.padding(top = Space.l))
                Text(
                    advice,
                    style = Type.body,
                    color = Palette.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Space.xxl, vertical = Space.xs),
                )
                (r as? ReadinessResult.Ready)?.let { ready -> basedOn(ready.used)?.let { Text(it.uppercase(), style = Type.tiny, color = Palette.Muted) } }
            }
        }
        item(key = "trio") {
            Column(Modifier.padding(horizontal = Space.xl)) {
                HorizontalDivider(color = Palette.Divider)
                Row(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    TrioStat("Sleep", data.sleep?.score, data.previous.sleep, Modifier.weight(1f)) { onOpen(1) }
                    TrioStat("Recovery", score, data.previous.readiness, Modifier.weight(1f), openRecovery)
                    TrioStat("Movement", data.movementScore, data.previous.movement, Modifier.weight(1f)) { onOpen(2) }
                }
            }
        }
        val a = data.activity
        if (data.sleep != null || data.naps.isNotEmpty() || a != null) {
            item(key = "day-label") { Heading(if (data.isToday) "Today" else "That day") }
        }
        data.sleep?.let { s ->
            item(key = "row-sleep") {
                ListRow(NavIcons.bed, "Sleep", "${formatDuration(s.asleep)} · ${formatTime12(s.start, zone)} – ${formatTime12(s.end, zone)}", Modifier.semantics { contentDescription = "Sleep details" }) { openSleep() }
            }
        }
        data.naps.forEachIndexed { i, nap ->
            item(key = "row-nap-$i") {
                ListRow(NavIcons.bed, "Nap", "${formatDuration(asleepDuration(nap))} · ${formatTime12(nap.start, zone)} – ${formatTime12(nap.end, zone)}") { onOpen(1) }
            }
        }
        if (a != null) {
            item(key = "row-steps") {
                ListRow(NavIcons.walk, "Steps", "${formatSteps(a.steps)} of ${formatSteps(data.goals.steps.toLong())} · ${a.goalPercent}%", Modifier.semantics { contentDescription = "Steps details" }) { openSteps() }
            }
        }
        item(key = "row-water") { WaterCard(data, onAddWater, onUndoWater) }
        val h = data.heart
        val tiles = buildList<@Composable (Modifier) -> Unit> {
            h?.hrv?.let { v ->
                add { m ->
                    StatTile(
                        "HRV", v.roundToInt().toString(), "ms", rangeNote(v, h.hrvRange, Good.HIGHER), m,
                        status = h.hrvRange?.let { rangeWord(rangeStatus(v, it, Good.HIGHER)) },
                        icon = NavIcons.activity,
                        onClick = openHrv,
                        description = "HRV details",
                    ) { h.hrvRange?.let { RangeBar("today-hrv", v, it, Good.HIGHER) } ?: EmptyTrack() }
                }
            }
            h?.restingHr?.let { v ->
                add { m ->
                    StatTile(
                        "Resting HR", v.roundToInt().toString(), "bpm", rangeNote(v, h.restingHrRange, Good.LOWER), m,
                        status = h.restingHrRange?.let { rangeWord(rangeStatus(v, it, Good.LOWER)) },
                        icon = NavIcons.heart,
                        labelTag = if (data.restingHrEstimated) "EST." else null,
                        onClick = openRhr,
                        description = "Resting heart rate details",
                    ) { h.restingHrRange?.let { RangeBar("today-rhr", v, it, Good.LOWER) } ?: EmptyTrack() }
                }
            }
        }
        if (tiles.isNotEmpty()) {
            item(key = "contributors-label") { SectionLabel("CONTRIBUTORS") }
            item(key = "contributors") { TileGrid(tiles) }
        }
        if (data.suggestions.isNotEmpty()) {
            item(key = "suggestions-label") { SectionLabel("DO THIS TODAY") }
            data.suggestions.forEachIndexed { i, s ->
                item(key = "suggestion-$i") { ListRow(suggestionIcon(s.kind), suggestionText(s), null) }
            }
        }
        item(key = "pills") {
            Row(Modifier.padding(horizontal = Space.l).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("SHARE DAY", Palette.Text, Palette.Divider, onShareDay, Modifier.weight(1f))
                Pill("✦ ASK AI", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), onAskAi, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WaterCard(data: TodayData, onAdd: (Int) -> Unit, onUndo: () -> Unit) {
    var adds by remember { mutableIntStateOf(0) }
    var canUndo by remember { mutableStateOf(false) }
    LaunchedEffect(adds) {
        if (adds > 0) {
            canUndo = true
            delay(UNDO_MILLIS)
            canUndo = false
        }
    }
    Column(Modifier.padding(horizontal = Space.l).fillMaxWidth().cardSurface().padding(horizontal = Space.l, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Palette.ChipStrong), contentAlignment = Alignment.Center) {
                Icon(NavIcons.drop, null, Modifier.size(16.dp), tint = Palette.Accent)
            }
            Column(Modifier.weight(1f).padding(horizontal = Space.m)) {
                Text("WATER", style = Type.label, color = Palette.Muted)
                Text(
                    "${formatLiters(data.waterLiters)} L of ${formatLiters(data.waterGoal)} L",
                    style = Type.small.copy(fontSize = 11.sp),
                    color = Palette.Text,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (data.isToday && canUndo) {
                Text(
                    "UNDO",
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { canUndo = false; onUndo() }.padding(Space.s),
                    style = Type.tiny,
                    color = Palette.Accent,
                )
            }
        }
        Box(Modifier.padding(top = Space.m).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Chart.track)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((data.waterLiters / data.waterGoal).toFloat().coerceIn(0f, 1f)).background(Palette.Accent))
        }
        if (data.isToday) {
            Row(Modifier.padding(top = Space.m).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(250, 500).forEach { ml ->
                    Pill("+$ml ML", Palette.Text, Palette.Divider, { adds++; onAdd(ml) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun TrioStat(label: String, value: Int?, previous: Int?, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val delta = if (value != null && previous != null && value != previous) value - previous else null
    Column(
        modifier
            .clip(RoundedCornerShape(Space.m))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label ${value ?: "no data"}" + when {
                    delta == null -> ""
                    delta > 0 -> ", up"
                    else -> ", down"
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), style = Type.tiny.copy(fontSize = 9.5.sp), color = Palette.Muted)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value?.toString() ?: "—", style = Type.title, color = if (value != null) Palette.Text else Palette.Faint)
            if (delta != null) {
                Text(if (delta > 0) "▲" else "▼", Modifier.padding(start = 3.dp), fontSize = 11.sp, color = if (delta > 0) Palette.Good else Palette.Bad)
            }
        }
    }
}

@Composable
private fun Pill(text: String, colour: Color, border: Color, onClick: () -> Unit, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)
    Text(
        text,
        modifier
            .clip(shape)
            .border(1.dp, border, shape)
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 13.dp),
        style = Type.label,
        color = colour,
        textAlign = TextAlign.Center,
    )
}
