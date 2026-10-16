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
import androidx.compose.foundation.shape.CircleShape
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
import app.fitdash.health.Workout
import app.fitdash.metrics.HeartDay
import app.fitdash.metrics.workoutIcon
import app.fitdash.ui.workoutTitle
import app.fitdash.ui.workoutLine
import app.fitdash.ui.components.DeltaArrow
import app.fitdash.ui.components.HeartDayLine
import app.fitdash.ui.components.NumberWithUnits
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.workoutIconVector
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import app.fitdash.ui.LocalPrefs
import app.fitdash.ui.regularityWord
import app.fitdash.ui.morningReadout
import app.fitdash.metrics.regularityBand
import app.fitdash.ui.theme.regularityColour
import app.fitdash.ui.theme.sleepScoreColour
import app.fitdash.ui.clock
import app.fitdash.ui.components.pressScale
import app.fitdash.ui.components.tappable
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.components.GlanceTile
import app.fitdash.ui.dataUpTo
import app.fitdash.ui.sourceAppName
import app.fitdash.ui.syncStale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.saveable.rememberSaveable
import app.fitdash.metrics.Suggestion
import app.fitdash.metrics.activeMinutes
import app.fitdash.ui.formatKm
import app.fitdash.ui.formatTime
import app.fitdash.ui.components.CardShape
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
import app.fitdash.ui.missingRecoveryLine
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
import app.fitdash.ui.components.MiniRing
import app.fitdash.ui.components.tappable
import app.fitdash.ui.components.cardSurface
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.components.WeekStrip
import app.fitdash.ui.formatDuration
import app.fitdash.ui.formatLiters
import app.fitdash.ui.formatSteps
import app.fitdash.ui.rangeNote
import app.fitdash.ui.suggestionText
import app.fitdash.ui.theme.LocalTheme
import app.fitdash.ui.theme.Palette
import app.fitdash.metrics.weekReview
import app.fitdash.ui.components.WeekReviewCard
import app.fitdash.ui.theme.Tint
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
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
    miles: Boolean = false,
    onOpenWorkout: (Workout) -> Unit = {},
    onSync: (String) -> Unit = {},
    refreshing: Boolean = false,
) {
    val r = data.readiness
    val score = (r as? ReadinessResult.Ready)?.score
    // While recovery calibrates the orb shows the best score we do have, so day one is not a dash.
    val hasSteps = (data.activity?.steps ?: 0L) > 0L
    val heroScore = score ?: data.sleep?.score ?: data.movementScore?.takeIf { hasSteps }
    val heroLabel = when {
        score != null -> "RECOVERY ›"
        data.sleep != null -> "SLEEP ›"
        hasSteps -> "STEPS ›"
        r is ReadinessResult.Calibrating -> "CALIBRATING"
        else -> "NO DATA"
    }
    val zone = ZoneId.systemDefault()
    val openRecovery = metricOpener(MetricRef.Kind(MetricKind.RECOVERY))
    val openSleep = metricOpener(MetricRef.Kind(MetricKind.SLEEP_HOURS))
    val openSteps = metricOpener(MetricRef.Kind(MetricKind.STEPS))
    val openHrv = metricOpener(MetricRef.Kind(MetricKind.HRV))
    val openRhr = metricOpener(MetricRef.Kind(MetricKind.RESTING_HR))
    val prefs = LocalPrefs.current
    PageColumn(chrome, accent = score?.let { scoreColor(it) } ?: Palette.Calibrating) {
        item(key = "strip") {
            WeekStrip(
                (6L downTo 0L).map { data.date.minusDays(it) },
                data.date,
                data.daysWithData,
                onSelectDate,
                enabled = { it <= chrome.today && it >= chrome.today.minusDays(chrome.daysBack) },
            )
        }
        if (data.isToday) item(key = "sync") { SyncRow(data, onSync, refreshing) }
        item(key = "orb") {
            if (LocalTheme.current.soft) {
                SoftHero(data, heroScore, heroLabel, pageOffset, when {
                    score != null -> openRecovery
                    data.sleep != null -> { { onOpen(1) } }
                    else -> { { onOpen(2) } }
                })
                return@item
            }
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
                val orbInteraction = remember { MutableInteractionSource() }
                val heroClick: () -> Unit = when {
                    score != null -> openRecovery
                    data.sleep != null -> { { onOpen(1) } }
                    else -> { { onOpen(2) } }
                }
                Orb(
                    heroScore,
                    Modifier
                        .padding(top = Space.xs)
                        .pressScale(orbInteraction, 0.96f)
                        .clickable(interactionSource = orbInteraction, indication = null, role = Role.Button, onClick = heroClick)
                        .semantics { contentDescription = heroLabel.trim('›', ' ').lowercase().replaceFirstChar { it.uppercase() } + " details" },
                    diameter = 172.dp,
                    intro = true,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (heroScore != null) CountUpText("orb-score", heroScore.toLong(), Type.hero) else Text("—", style = Type.hero)
                        Text(heroLabel, style = Type.label)
                    }
                }
                val (word, advice) = heroWords(data)
                Text(word.uppercase(), style = Type.label, color = score?.let { scoreColor(it) } ?: Palette.Muted, modifier = Modifier.padding(top = Space.m))
                // Seven dots that fill night by night, so calibrating reads as progress rather than waiting.
                if (r is ReadinessResult.Calibrating) {
                    Row(Modifier.padding(top = Space.s), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(Tuning.MIN_NIGHTS) { i ->
                            Box(
                                Modifier
                                    .size(if (i < r.nights) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (i < r.nights) Palette.Accent else Palette.Divider),
                            )
                        }
                    }
                }
                Text(
                    advice,
                    style = Type.body,
                    color = Palette.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Space.xxl, vertical = Space.xs),
                )
                (r as? ReadinessResult.Ready)?.let { ready -> basedOn(ready.used)?.let { Text(it.uppercase(), style = Type.tiny, color = Palette.Muted) } }
                if (data.isToday) missingRecoveryLine(data.sources)?.let {
                    Text(it, style = Type.secondary, color = Palette.Warn, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Space.xxl, vertical = Space.xs))
                }
            }
        }
        // Monday: last week in one card, the week-over-week view that otherwise lives on Trends.
        if (data.isToday && data.date.dayOfWeek == java.time.DayOfWeek.MONDAY) {
            weekReview(data.series, data.date.minusDays(1))?.let { review -> item(key = "week-review") { WeekReviewCard(review) } }
        }
        item(key = "glance") { GlanceGrid(data, score, onOpen, openSteps, openRhr, metricOpener(MetricRef.Kind(MetricKind.ACTIVE_MINUTES)), metricOpener(MetricRef.Kind(MetricKind.HEART_RATE))) }
        // Action first: the one thing to do today sits right under the numbers, the rest fold under it.
        if (prefs.showSuggestions && data.suggestions.isNotEmpty()) {
            item(key = "suggestions-label") { SectionLabel("DO THIS TODAY") }
            item(key = "suggestion-0") { ActionCard(data.suggestions.first()) }
            data.suggestions.drop(1).forEachIndexed { i, s ->
                item(key = "suggestion-${i + 1}") { ListRow(suggestionIcon(s.kind), suggestionText(s), null) }
            }
        }
        val a = data.activity
        if (data.sleep != null || data.naps.isNotEmpty() || a != null) {
            item(key = "day-label") { Heading(if (data.isToday) "Today" else "That day") }
        }
        data.sleep?.let { s ->
            item(key = "row-sleep") {
                ListRow(NavIcons.bed, "Sleep", "${formatDuration(s.asleep)} · ${clock(s.start, zone)} – ${clock(s.end, zone)}", Modifier.semantics { contentDescription = "Sleep details" }) { openSleep() }
            }
        }
        data.naps.forEachIndexed { i, nap ->
            item(key = "row-nap-$i") {
                ListRow(NavIcons.bed, "Nap", "${formatDuration(asleepDuration(nap))} · ${clock(nap.start, zone)} – ${clock(nap.end, zone)}") { onOpen(1) }
            }
        }
        if (a != null) {
            item(key = "row-steps") {
                ListRow(NavIcons.walk, "Steps", "${formatSteps(a.steps)} of ${formatSteps(data.goals.steps.toLong())} · ${a.goalPercent}%", Modifier.semantics { contentDescription = "Steps details" }) { openSteps() }
            }
        }
        if (prefs.showWorkouts && data.workouts.isNotEmpty()) {
            if (data.workouts.size == 1) {
                val w = data.workouts.first()
                item(key = "row-workout-0") {
                    ListRow(workoutIconVector(workoutIcon(w.type)), workoutTitle(w), workoutLine(w, zone, miles, prefs.clock24), Modifier.semantics { contentDescription = "${workoutTitle(w)} details" }) { onOpenWorkout(w) }
                }
            } else {
                // Several sessions fold into one row: "4 workouts · 78 min · 7.7 km", tap to expand.
                item(key = "row-workouts") { WorkoutsRow(data, miles, onOpenWorkout) }
            }
        }
        if (prefs.showWater) item(key = "row-water") { WaterCard(data, onAddWater, onUndoWater) }
        if (prefs.showHeartRate) data.heartDay?.let { hd ->
            item(key = "heart-day") {
                val openHr = metricOpener(MetricRef.Kind(MetricKind.HEART_RATE))
                HeartDayCard(data, hd, zone, openHr)
            }
        }
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
                        tint = Tint.LAVENDER,
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
                        tint = Tint.PEACH,
                    ) { h.restingHrRange?.let { RangeBar("today-rhr", v, it, Good.LOWER) } ?: EmptyTrack() }
                }
            }
        }
        if (tiles.isNotEmpty()) {
            item(key = "contributors-label") { SectionLabel("CONTRIBUTORS") }
            item(key = "contributors") { TileGrid(tiles) }
        }
        item(key = "pills") {
            Row(Modifier.padding(horizontal = Space.l).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("SHARE DAY", Palette.Text, Palette.Divider, onShareDay, Modifier.weight(1f))
                Pill("ASK AI", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), onAskAi, Modifier.weight(1f), icon = NavIcons.sparkle)
            }
        }
    }
}

@Composable
private fun ActionCard(s: Suggestion) {
    Row(
        Modifier.padding(horizontal = Space.l).fillMaxWidth().clip(CardShape).background(Palette.ChipStrong).padding(horizontal = Space.l, vertical = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(suggestionIcon(s.kind), null, Modifier.padding(end = Space.m).size(22.dp), tint = Palette.Accent)
        Text(suggestionText(s), style = Type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp), color = Palette.Text)
    }
}

@Composable
private fun WorkoutsRow(data: TodayData, miles: Boolean, onOpenWorkout: (Workout) -> Unit) {
    var expanded by rememberSaveable(data.date) { mutableStateOf(false) }
    val prefs = LocalPrefs.current
    val zone = ZoneId.systemDefault()
    val minutes = activeMinutes(data.workouts)
    val km = data.workouts.mapNotNull { it.distanceMeters }.sum() / 1000
    val subtitle = listOfNotNull("${minutes}m", km.takeIf { it > 0 }?.let { formatKm(it, miles) }).joinToString(" · ")
    Column(Modifier.animateContentSize()) {
        ListRow(NavIcons.flame, "${data.workouts.size} workouts", subtitle, Modifier.semantics { contentDescription = "${data.workouts.size} workouts, ${if (expanded) "collapse" else "expand"}" }) { expanded = !expanded }
        if (expanded) {
            data.workouts.forEach { w ->
                ListRow(
                    workoutIconVector(workoutIcon(w.type)), workoutTitle(w), workoutLine(w, zone, miles, prefs.clock24),
                    Modifier.padding(top = Space.s).semantics { contentDescription = "${workoutTitle(w)} details" },
                ) { onOpenWorkout(w) }
            }
        }
    }
}

// "DATA UP TO 9:43 PM · SYNC": tells the user how fresh the watch data is and opens the wearable app to push more.
@Composable
private fun SyncRow(data: TodayData, onSync: (String) -> Unit, refreshing: Boolean) {
    val prefs = LocalPrefs.current
    val now = remember(data.date) { java.time.ZonedDateTime.now() }
    val stale = syncStale(data.latestData, now)
    // Names and dates ("Fitbit", "13 Oct") stay as written on Soft; the dark themes set the row in capitals.
    val soft = LocalTheme.current.soft
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.xl), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(
            (if (refreshing) "Updating…" else data.latestData?.let { dataUpTo(it, now, prefs.clock24) } ?: "No watch data yet").let { if (soft) it else it.uppercase() },
            style = Type.tiny,
            color = if (refreshing) Palette.Muted else if (stale) Palette.Warn else Palette.Faint,
        )
        data.syncApp?.let { pkg ->
            Text(
                if (soft) "Sync ${sourceAppName(pkg)}" else "SYNC ${sourceAppName(pkg).uppercase()}",
                Modifier
                    .padding(start = Space.s)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (stale) Palette.Warn.copy(alpha = 0.14f) else Palette.Chip)
                    .tappable({ onSync(pkg) }, "Open ${sourceAppName(pkg)} to sync")
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = Type.tiny.copy(fontWeight = FontWeight.Medium),
                color = if (stale) Palette.Warn else Palette.Accent,
            )
        }
    }
}

// Four tiles under the orb: sleep, steps, active minutes and the heart number we have.
@Composable
private fun GlanceGrid(data: TodayData, recovery: Int?, onOpen: (Int) -> Unit, openSteps: () -> Unit, openRhr: () -> Unit, openActive: () -> Unit, openHeart: () -> Unit) {
    val a = data.activity
    val h = data.heart
    val sleepDelta = data.sleep?.score?.let { s -> data.previous.sleep?.let { s - it } }
    val stepsDelta = data.movementScore?.takeIf { (a?.steps ?: 0L) > 0L }?.let { m -> data.previous.movement?.let { m - it } }
    val tiles = listOf<@Composable (Modifier) -> Unit>(
        { m ->
            val s = data.sleep
            if (LocalPrefs.current.showSleepScore) {
                GlanceTile(
                    "Sleep", s?.score?.toString() ?: "—", null, m, icon = NavIcons.moonSmall,
                    ring = s?.score?.div(100f), ringColor = s?.score?.let { sleepScoreColour(it) } ?: Palette.Faint,
                    note = s?.let { formatDuration(it.asleep) } ?: "no night yet", delta = sleepDelta,
                    ringKey = "glance-sleep-${data.date}", onClick = { onOpen(1) }, description = "Sleep page", tint = Tint.SKY,
                )
            } else {
                val band = data.regularity?.let { regularityBand(it.value.sri) }
                GlanceTile(
                    "Sleep", s?.let { formatDuration(it.asleep) } ?: "—", null, m, icon = NavIcons.moonSmall,
                    ring = s?.let { it.goalPercent / 100f }, ringColor = band?.let { regularityColour(it) } ?: Palette.Accent,
                    note = band?.let(::regularityWord) ?: (s?.let { "calibrating" } ?: "no night yet"),
                    ringKey = "glance-sleep-${data.date}", onClick = { onOpen(1) }, description = "Sleep page", tint = Tint.SKY,
                )
            }
        },
        { m ->
            GlanceTile(
                "Steps", a?.let { formatSteps(it.steps) } ?: "—", null, m, icon = NavIcons.walk,
                ring = a?.goalPercent?.coerceAtMost(100)?.div(100f), ringColor = Palette.Accent,
                note = "of ${formatSteps(data.goals.steps.toLong())}", delta = stepsDelta,
                ringKey = "glance-steps-${data.date}", onClick = openSteps, description = "Steps details", tint = Tint.SAGE,
            )
        },
        { m ->
            val goal = data.goals.activeMinutes
            GlanceTile(
                "Active", a?.activeMinutes?.toString() ?: "—", if (a != null) "min" else null, m, icon = NavIcons.bolt,
                ring = a?.let { (it.activeMinutes.toFloat() / goal).coerceAtMost(1f) }, ringColor = Palette.Good,
                note = "goal $goal min",
                ringKey = "glance-active-${data.date}", onClick = openActive, description = "Active minutes details", tint = Tint.LAVENDER,
            )
        },
        { m ->
            val rhr = h?.restingHr
            val hd = data.heartDay
            when {
                rhr != null -> GlanceTile(
                    "Resting HR", rhr.roundToInt().toString(), "bpm", m, icon = NavIcons.heart,
                    status = h.restingHrRange?.let { rangeWord(rangeStatus(rhr, it, Good.LOWER)) },
                    note = if (data.restingHrEstimated) "estimated" else "last night",
                    onClick = openRhr, description = "Resting heart rate details", tint = Tint.PEACH,
                )
                hd != null -> GlanceTile("Heart rate", hd.avg.toString(), "bpm", m, icon = NavIcons.heart, note = "${hd.min}–${hd.max} today", onClick = openHeart, description = "Heart rate details", tint = Tint.PEACH)
                else -> GlanceTile(
                    "Recovery", recovery?.toString() ?: "—", null, m, icon = NavIcons.today,
                    ring = recovery?.div(100f), ringColor = recovery?.let { scoreColor(it) } ?: Palette.Faint,
                    note = if (recovery == null) "calibrating" else null, ringKey = "glance-recovery-${data.date}", tint = Tint.PEACH,
                )
            }
        },
    )
    TileGrid(tiles)
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
    Column(Modifier.padding(horizontal = Space.l).fillMaxWidth().cardSurface().padding(horizontal = Space.l, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Palette.ChipStrong), contentAlignment = Alignment.Center) {
                Icon(NavIcons.drop, null, Modifier.size(16.dp), tint = Palette.Accent)
            }
            Column(Modifier.weight(1f).padding(horizontal = Space.m)) {
                Text("Water", style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = Palette.Text)
                Text("${formatLiters(data.waterLiters)} of ${formatLiters(data.waterGoal)} L", style = Type.secondary, color = Palette.Muted, modifier = Modifier.padding(top = 2.dp))
            }
            if (data.isToday && canUndo) {
                Text(
                    "UNDO",
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { canUndo = false; onUndo() }.padding(Space.s),
                    style = Type.tiny,
                    color = Palette.Accent,
                )
            } else if (data.isToday) {
                // Compact chips instead of two full-width pills.
                listOf(250, 500).forEach { ml ->
                    Text(
                        "+$ml",
                        Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Palette.Chip)
                            .tappable({ adds++; onAdd(ml) }, "Add $ml millilitres")
                            .heightIn(min = 40.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        style = Type.label,
                        color = Palette.Accent,
                    )
                }
            }
        }
        val fill by animateFloatAsState((data.waterLiters / data.waterGoal).toFloat().coerceIn(0f, 1f), tween(Motion.RING, easing = Motion.ease), label = "water")
        Box(Modifier.padding(top = Space.m).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Chart.track)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Palette.Accent))
        }
    }
}

@Composable
internal fun TrioStat(label: String, value: Int?, previous: Int?, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val delta = if (value != null && previous != null && value != previous) value - previous else null
    Column(
        modifier
            .clip(RoundedCornerShape(Space.m))
            .tappable(onClick, null)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label ${value ?: "no data"}" + when {
                    delta == null -> ""
                    delta > 0 -> ", up"
                    else -> ", down"
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), style = Type.tiny, color = Palette.Muted)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            // Day changes slide the number up or down instead of snapping.
            AnimatedContent(
                value,
                transitionSpec = {
                    val up = (targetState ?: 0) >= (initialState ?: 0)
                    (slideInVertically { if (up) it / 2 else -it / 2 } + fadeIn()) togetherWith (slideOutVertically { if (up) -it / 2 else it / 2 } + fadeOut())
                },
                label = "trio-$label",
            ) { v ->
                Text(v?.toString() ?: "—", style = Type.title, color = if (v != null) Palette.Text else Palette.Faint)
            }
            if (delta != null) DeltaArrow(delta > 0, Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun Pill(text: String, colour: Color, border: Color, onClick: () -> Unit, modifier: Modifier, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .clip(shape)
            .border(1.dp, border, shape)
            .tappable(onClick, null)
            .heightIn(min = 48.dp)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, Modifier.padding(end = 6.dp).size(12.dp), tint = colour)
        Text(text, style = Type.label, color = colour, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HeartDayCard(data: TodayData, hd: HeartDay, zone: ZoneId, onOpen: () -> Unit) {
    fun frac(t: java.time.Instant): Float {
        val z = t.atZone(zone)
        if (z.toLocalDate() < data.date) return 0f
        if (z.toLocalDate() > data.date) return 1f
        return (z.hour * 60 + z.minute) / (24f * 60f)
    }
    val sleepSpans = listOfNotNull(data.sleep?.let { frac(it.start) to frac(it.end) }) + data.naps.map { frac(it.start) to frac(it.end) }
    val workoutSpans = data.workouts.map { frac(it.start) to frac(it.end) }
    val latestLabel = if (data.isToday) "latest ${hd.latest.value.roundToInt()} bpm" else "last ${hd.latest.value.roundToInt()} bpm"
    val clock24 = LocalPrefs.current.clock24
    SectionCard("HEART RATE", trailing = latestLabel, onClick = onOpen, description = "Heart rate details") {
        HeartDayLine(
            "heart-day-${data.date}",
            hd.line,
            Modifier.fillMaxWidth().height(64.dp),
            restingHr = data.heart?.restingHr,
            sleepSpans = sleepSpans.filter { it.second > it.first },
            workoutSpans = workoutSpans.filter { it.second > it.first },
            readout = { slot, v ->
                val m = slot * Tuning.HR_BUCKET_MINUTES
                val t = data.date.atTime(m / 60, m % 60).atZone(zone).toInstant()
                "${formatTime(t, zone, clock24)} · ${v.roundToInt()} bpm"
            },
        )
        Row(Modifier.fillMaxWidth().padding(top = Space.xs), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach { Text(it, style = Type.tiny, color = Palette.Faint) }
        }
        Row(Modifier.fillMaxWidth().padding(top = Space.s), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("MIN" to hd.min, "AVG" to hd.avg, "MAX" to hd.max).forEach { (label, v) ->
                Column {
                    Text(label, style = Type.tiny, color = Palette.Muted)
                    NumberWithUnits(listOf(v.toString() to "bpm"), style = Type.tile.copy(fontSize = 20.sp))
                }
            }
        }
    }
}

// Soft's hero: a white card with a thick score ring on the left and the verdict beside it, in place of the orb.
@Composable
private fun SoftHero(data: TodayData, heroScore: Int?, heroLabel: String, pageOffset: () -> Float, onClick: () -> Unit) {
    val r = data.readiness
    val score = (r as? ReadinessResult.Ready)?.score
    val (word, advice) = heroWords(data)
    val name = heroLabel.trim('›', ' ').lowercase()
    Row(
        Modifier
            .padding(horizontal = Space.l)
            .fillMaxWidth()
            .graphicsLayer {
                val offset = pageOffset()
                translationX = offset * size.width * 0.15f
                alpha = 1f - abs(offset).coerceAtMost(1f)
            }
            .cardSurface()
            .tappable(onClick, name.replaceFirstChar { it.uppercase() } + " details")
            .padding(horizontal = Space.l, vertical = 18.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            MiniRing("hero-ring-${data.date}", (heroScore ?: 0) / 100f, heroScore?.let { scoreColor(it) } ?: Palette.Faint, diameter = 112.dp, stroke = 12.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (heroScore != null) CountUpText("orb-score", heroScore.toLong(), Type.title.copy(fontSize = 34.sp), color = Palette.Text)
                else Text("—", style = Type.title.copy(fontSize = 34.sp), color = Palette.Faint)
                Text(name, style = Type.secondary, color = Palette.Muted)
            }
        }
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Text(word, style = Type.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium), color = Palette.Text)
            Text(advice, style = Type.secondary.copy(fontSize = 13.sp, lineHeight = 18.sp), color = Palette.Muted, modifier = Modifier.padding(top = Space.xs))
            if (r is ReadinessResult.Calibrating) {
                Row(Modifier.padding(top = Space.s), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(Tuning.MIN_NIGHTS) { i ->
                        Box(Modifier.size(7.dp).clip(CircleShape).background(if (i < r.nights) Palette.Accent else Palette.Divider))
                    }
                }
            }
            (r as? ReadinessResult.Ready)?.let { ready -> basedOn(ready.used)?.let { Text(it, style = Type.tiny, color = Palette.Muted, modifier = Modifier.padding(top = Space.s)) } }
            if (data.isToday) missingRecoveryLine(data.sources)?.let { Text(it, style = Type.secondary, color = Palette.Warn, modifier = Modifier.padding(top = Space.s)) }
        }
    }
}

// The hero's word and its line: the morning readout when recovery is ready, the countdown while calibrating.
private fun heroWords(data: TodayData): Pair<String, String> {
    val readout = if (data.isToday) morningReadout(data.sleep?.asleep, data.heart?.restingHr, data.heart?.restingHrRange, data.heart?.hrv, data.heart?.hrvRange, data.suggestions.firstOrNull()) else null
    val usable = listOfNotNull(data.sleep?.let { "sleep" }, data.activity?.let { "steps" }, if (data.regularity != null) "sleep regularity" else null)
    return when (val r = data.readiness) {
        is ReadinessResult.Ready -> bandWord(r.band) to (readout ?: bandAdvice(r.band))
        is ReadinessResult.Calibrating -> {
            val left = Tuning.MIN_NIGHTS - r.nights
            "Recovery · night ${r.nights} of ${Tuning.MIN_NIGHTS}" to
                if (data.isToday) "Your baseline is forming: $left more ${if (left == 1) "night" else "nights"} of sleep, HRV and resting heart rate." + (if (usable.isNotEmpty()) " Usable now: ${usable.joinToString(", ")}." else "") else "Not enough earlier data for this day"
        }
        ReadinessResult.NoData -> "No data for last night" to "Sync your watch, then pull down to refresh."
    }
}
