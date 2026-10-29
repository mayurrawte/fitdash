package app.fitdash.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.fitdash.health.SleepSession
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.wakingWindow
import app.fitdash.metrics.waterDue
import app.fitdash.settings.JournalStore
import app.fitdash.settings.NutrientKind
import app.fitdash.ui.LocalPrefs
import app.fitdash.ui.clock
import app.fitdash.ui.components.Bottle
import app.fitdash.ui.components.HeroCard
import app.fitdash.ui.components.ListRow
import app.fitdash.ui.components.MiniRing
import app.fitdash.ui.components.NumberWithUnits
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.Pill
import app.fitdash.ui.components.RowGroup
import app.fitdash.ui.components.SegmentedControl
import app.fitdash.ui.components.StripCard
import app.fitdash.ui.components.tappable
import app.fitdash.ui.formatLiters
import app.fitdash.ui.formatTime
import app.fitdash.ui.theme.LocalTheme
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Tint
import app.fitdash.ui.theme.Type
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

private const val UNDO_MILLIS = 3000L

@Composable
fun LogPage(
    data: TodayData,
    chrome: PageChrome,
    journal: JournalStore,
    onAddWater: (Int) -> Unit,
    onUndoWater: () -> Unit,
    onAddNutrient: (NutrientKind, Double) -> Unit,
    onRemoveNutrient: (NutrientKind, Instant) -> Unit,
    playBuild: Boolean,
) {
    val prefs = LocalPrefs.current
    val haptic = LocalHapticFeedback.current
    val segments = if (playBuild) listOf("Water", "Tags") else listOf("Water", "Protein", "Caffeine", "Tags")
    var segment by rememberSaveable { mutableIntStateOf(0) }
    // This session's adds only: Health Connect does not hand back which water records are ours.
    var entries by rememberSaveable { mutableStateOf(listOf<String>()) }
    var lastMl by remember { mutableIntStateOf(0) }
    var adds by remember { mutableIntStateOf(0) }
    var canUndo by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    val days by journal.flow.collectAsState()
    val day = days[data.date]
    val zone = ZoneId.systemDefault()
    LaunchedEffect(adds) {
        if (adds > 0) {
            canUndo = true
            delay(UNDO_MILLIS)
            canUndo = false
        }
    }
    val add = { ml: Int ->
        onAddWater(ml)
        entries = listOf("Added $ml ml · ${formatTime(Instant.now(), ZoneId.systemDefault(), prefs.clock24)}") + entries
        lastMl = ml
        adds++
    }
    Box(Modifier.fillMaxSize()) {
        PageColumn(chrome, accent = Palette.Rem) {
            item(key = "segments") { SegmentedControl(segments, segment, { segment = it }) }
            if (segments[segment] == "Water") {
                revealItem("water", 0) {
                    HeroCard(null, "Water", tint = Tint.WATER) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Bottle((data.waterLiters / data.waterGoal).toFloat())
                            Column(Modifier.padding(start = Space.l)) {
                                NumberWithUnits(listOf(formatLiters(data.waterLiters) to "L"), style = Type.hero)
                                Text("of ${formatLiters(data.waterGoal)} L", style = Type.body, color = Palette.Muted)
                                if (data.isToday) {
                                    val now = ZonedDateTime.now()
                                    val window = wakingWindow(listOfNotNull(data.sleep?.let { SleepSession(it.start, it.end, it.stages) }), now, data.goals.sleepHours)
                                    val behind = waterDue(data.waterLiters, data.waterGoal, now, window, null)
                                    Text(
                                        if (behind) "Behind" else "On pace",
                                        Modifier.padding(top = Space.s).clip(RoundedCornerShape(8.dp)).background(Palette.Chip).padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = Type.tiny.copy(fontWeight = FontWeight.Medium),
                                        color = if (behind) Palette.Warn else Palette.Text,
                                    )
                                }
                            }
                        }
                    }
                }
                if (data.isToday) {
                    item(key = "water-add") {
                        Row(Modifier.padding(horizontal = Space.l).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Pill("+250", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), { add(250) }, Modifier.weight(1f))
                            Pill("+500", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), { add(500) }, Modifier.weight(1f))
                            Pill("Custom", Palette.Text, Palette.Divider, { custom = true }, Modifier.weight(1f))
                        }
                    }
                    if (entries.isNotEmpty()) item(key = "water-entries") {
                        Column(Modifier.padding(horizontal = Space.xl), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                            entries.forEach { Text(it, style = Type.secondary, color = Palette.Muted) }
                        }
                    }
                }
            } else if (segments[segment] == "Protein" || segments[segment] == "Caffeine") {
                val kind = if (segments[segment] == "Protein") NutrientKind.PROTEIN else NutrientKind.CAFFEINE
                val list = (if (kind == NutrientKind.PROTEIN) day?.protein else day?.caffeine).orEmpty()
                if (kind == NutrientKind.PROTEIN) revealItem("protein", 0) {
                    val grams = list.sumOf { it.amount }
                    HeroCard(null, "Protein", tint = Tint.PEACH) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiniRing("protein", (grams / prefs.proteinGoalG).toFloat(), Palette.Text, diameter = 72.dp, stroke = 7.dp)
                            Column(Modifier.padding(start = Space.l)) {
                                NumberWithUnits(listOf(grams.roundToInt().toString() to "g"), style = Type.hero)
                                Text("of ${prefs.proteinGoalG} g", style = Type.body, color = Palette.Muted)
                            }
                        }
                    }
                } else revealItem("caffeine", 0) {
                    HeroCard(null, "Caffeine", tint = Tint.SAND) {
                        Column {
                            NumberWithUnits(listOf(list.size.toString() to if (list.size == 1) "cup" else "cups"), style = Type.hero)
                            Text(list.maxOfOrNull { it.at }?.let { "Last at ${clock(it, zone)}" } ?: "None today", style = Type.body, color = Palette.Muted)
                        }
                    }
                }
                if (data.isToday) item(key = "${kind.name}-add") {
                    Row(Modifier.padding(horizontal = Space.l).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (kind == NutrientKind.PROTEIN) {
                            Pill("+10 g", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), { onAddNutrient(kind, 10.0) }, Modifier.weight(1f))
                            Pill("+25 g", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), { onAddNutrient(kind, 25.0) }, Modifier.weight(1f))
                            Pill("Custom", Palette.Text, Palette.Divider, { custom = true }, Modifier.weight(1f))
                        } else {
                            Pill("+1 coffee", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), { onAddNutrient(kind, 95.0) }, Modifier.weight(1f))
                            Pill("+1 tea", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), { onAddNutrient(kind, 47.0) }, Modifier.weight(1f))
                        }
                    }
                }
                if (list.isNotEmpty()) item(key = "${kind.name}-entries") {
                    RowGroup {
                        list.sortedByDescending { it.at }.forEachIndexed { i, entry ->
                            val amount = if (kind == NutrientKind.PROTEIN) "${entry.amount.roundToInt()} g" else "${entry.amount.roundToInt()} mg"
                            ListRow(
                                if (kind == NutrientKind.PROTEIN) NavIcons.bolt else NavIcons.cup, amount, clock(entry.at, zone),
                                trailing = {
                                    Text(
                                        "Remove",
                                        Modifier.clip(RoundedCornerShape(8.dp)).tappable({ onRemoveNutrient(kind, entry.at) }, "Remove $amount").padding(Space.s),
                                        style = Type.label,
                                        color = Palette.Accent,
                                    )
                                },
                                divider = i > 0,
                            )
                        }
                    }
                }
            } else {
                item(key = "soon") { Text("Coming in the next step", Modifier.padding(horizontal = Space.xl), style = Type.body, color = Palette.Muted) }
            }
        }
        AnimatedVisibility(
            canUndo,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            StripCard(null, null) {
                Text("Added $lastMl ml", Modifier.weight(1f), style = Type.body, color = Palette.Text)
                Text(
                    "Undo",
                    Modifier.clip(RoundedCornerShape(8.dp)).tappable({ canUndo = false; entries = entries.drop(1); onUndoWater() }, "Undo water").padding(Space.s),
                    style = Type.label,
                    color = Palette.Accent,
                )
            }
        }
    }
    if (custom) {
        val protein = segments[segment] == "Protein"
        var ml by remember { mutableStateOf("") }
        val amount = ml.toIntOrNull()?.takeIf { it > 0 }
        AlertDialog(
            onDismissRequest = { custom = false },
            containerColor = LocalTheme.current.bgMid,
            title = { Text(if (protein) "PROTEIN" else "WATER", style = Type.label, color = Palette.Muted) },
            text = { NumberField(ml, if (protein) "g" else "ml", Modifier.fillMaxWidth(), digits = if (protein) 3 else 4) { ml = it } },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (protein) onAddNutrient(NutrientKind.PROTEIN, amount!!.toDouble()) else add(amount!!)
                        custom = false
                    },
                    enabled = amount != null,
                ) { Text("ADD", style = Type.label, color = if (amount != null) Palette.Accent else Palette.Faint) }
            },
            dismissButton = { TextButton(onClick = { custom = false }) { Text("CANCEL", style = Type.label, color = Palette.Muted) } },
        )
    }
}
