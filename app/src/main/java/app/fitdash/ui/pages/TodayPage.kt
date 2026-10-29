package app.fitdash.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fitdash.health.ReadProgress
import app.fitdash.health.StageType
import app.fitdash.health.Workout
import app.fitdash.metrics.Good
import app.fitdash.metrics.MetricKind
import app.fitdash.metrics.ReadinessResult
import app.fitdash.metrics.SuggestionKind
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
import app.fitdash.metrics.activeMinutes
import app.fitdash.metrics.rangeStatus
import app.fitdash.metrics.rangeWord
import app.fitdash.metrics.weekReview
import app.fitdash.metrics.workoutIcon
import app.fitdash.ui.LocalPrefs
import app.fitdash.ui.bandAdvice
import app.fitdash.ui.bandWord
import app.fitdash.ui.basedOn
import app.fitdash.ui.dataUpTo
import app.fitdash.ui.formatDate
import app.fitdash.ui.formatKm
import app.fitdash.ui.formatLiters
import app.fitdash.ui.formatSteps
import app.fitdash.ui.greeting
import app.fitdash.ui.initial
import app.fitdash.ui.missingRecoveryLine
import app.fitdash.ui.morningReadout
import app.fitdash.ui.rangeNote
import app.fitdash.ui.sourceAppName
import app.fitdash.ui.streakLine
import app.fitdash.ui.suggestionText
import app.fitdash.ui.syncStale
import app.fitdash.ui.workoutLine
import app.fitdash.ui.workoutTitle
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.CountUpText
import app.fitdash.ui.components.DeltaArrow
import app.fitdash.ui.components.EmptyTrack
import app.fitdash.ui.components.HeartDayLine
import app.fitdash.ui.components.HeroCard
import app.fitdash.ui.components.Hypnogram
import app.fitdash.ui.components.ListRow
import app.fitdash.ui.components.MiniRing
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.NumberWithUnits
import app.fitdash.ui.components.RangeBar
import app.fitdash.ui.components.RowGroup
import app.fitdash.ui.components.StatTile
import app.fitdash.ui.components.StripCard
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.components.WeekReviewCard
import app.fitdash.ui.components.WeekStrip
import app.fitdash.ui.components.tappable
import app.fitdash.ui.components.workoutIconVector
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Tint
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    progress: StateFlow<ReadProgress?> = MutableStateFlow(null),
) {
    val r = data.readiness
    val score = (r as? ReadinessResult.Ready)?.score
    val a = data.activity
    val h = data.heart
    val hasSteps = (a?.steps ?: 0L) > 0L
    val zone = ZoneId.systemDefault()
    val prefs = LocalPrefs.current
    val sleepStreak = data.streaks.firstOrNull { it.kind == MetricKind.SLEEP_HOURS }?.let { streakLine(it.days) }
    val stepsStreak = data.streaks.firstOrNull { it.kind == MetricKind.STEPS }?.let { streakLine(it.days) }
    val waterStreak = data.streaks.firstOrNull { it.kind == MetricKind.WATER }?.let { streakLine(it.days) }
    val goal = data.goals.steps
    val goalText = if (goal % 1000 == 0) "of ${goal / 1000}k" else "of ${formatSteps(goal.toLong())}"
    PageColumn(chrome, accent = score?.let { scoreColor(it) } ?: Palette.Calibrating) {
        item(key = "greeting") {
            Row(Modifier.padding(horizontal = Space.xl, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (data.isToday) greeting(LocalTime.now().hour, prefs.name) else formatDate(data.date),
                    style = Type.title,
                    color = Palette.Text,
                )
                Spacer(Modifier.weight(1f))
                val name = prefs.name
                if (data.isToday && name != null) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Palette.Accent), contentAlignment = Alignment.Center) {
                        Text(initial(name), style = Type.body.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
            }
        }
        item(key = "strip") {
            WeekStrip(
                (6L downTo 0L).map { data.date.minusDays(it) },
                data.date,
                data.daysWithData,
                onSelectDate,
                enabled = { it <= chrome.today && it >= chrome.today.minusDays(chrome.daysBack) },
            )
        }
        if (data.isToday) item(key = "sync") { SyncRow(data, onSync, refreshing, progress, chrome.error) }
        revealItem("readout", 0) {
            StripCard(null, null) {
                Icon(NavIcons.sparkle, null, Modifier.padding(end = Space.m).size(18.dp), tint = Palette.Accent)
                Column {
                    Text(readout(data), style = Type.body, color = Palette.Text)
                    (data.readiness as? ReadinessResult.Ready)?.let { ready -> basedOn(ready.used)?.let { Text(it, style = Type.small, color = Palette.Muted, modifier = Modifier.padding(top = Space.xs)) } }
                    if (data.isToday) missingRecoveryLine(data.sources)?.let { Text(it, style = Type.secondary, color = Palette.Warn, modifier = Modifier.padding(top = Space.xs)) }
                }
            }
        }
        revealItem("hero", 1) {
            val s = data.sleep
            val onSteps = s == null && hasSteps
            HeroCard(
                when { s != null -> { { onOpen(1) } }; hasSteps -> { { onOpen(2) } }; else -> chrome.onInfo },
                when { s != null -> "Sleep details"; hasSteps -> "Steps details"; else -> "Data sources" },
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (onSteps) NavIcons.walk else NavIcons.moonSmall, null, Modifier.padding(end = 6.dp).size(14.dp), tint = Palette.Muted)
                    Text(if (onSteps) "Steps" else "Sleep", Modifier.weight(1f), style = Type.body.copy(fontSize = 13.sp), color = Palette.Muted)
                    Icon(NavIcons.chevronRight, null, Modifier.size(16.dp), tint = Palette.Muted)
                }
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(top = 28.dp), verticalAlignment = Alignment.Bottom) {
                    Column {
                        when {
                            s != null -> NumberWithUnits(listOf(s.asleep.toHours().toString() to "h", (s.asleep.toMinutes() % 60).toString() to "m"), style = Type.tile.copy(fontSize = 46.sp))
                            a != null && hasSteps -> NumberWithUnits(listOf(formatSteps(a.steps) to goalText), style = Type.tile.copy(fontSize = 46.sp))
                            else -> Text("No sleep recorded", style = Type.value, color = Palette.Text)
                        }
                        val line = if (s != null) sleepStreak else if (hasSteps) stepsStreak else "Check your data sources"
                        if (line != null) Text(line, style = Type.small, color = Palette.Muted)
                    }
                    if (s != null) Hypnogram(
                        "hero-hypnogram-${data.date}",
                        s.stages,
                        Modifier.padding(start = Space.m, bottom = 6.dp).weight(1f).height(70.dp),
                        colours = StageType.entries.associateWith { if (it == StageType.AWAKE) Palette.Sun else Color.White },
                    )
                }
            }
        }
        val tiles = buildList<@Composable (Modifier) -> Unit> {
            // Left out when the hero shows steps (no sleep, steps above zero).
            if (a != null && (data.sleep != null || !hasSteps)) add { m ->
                StatTile("Steps", formatSteps(a.steps), goalText, null, m, icon = NavIcons.walk, onClick = metricOpener(MetricRef.Kind(MetricKind.STEPS)), description = "Steps details", tint = Tint.SKY) {
                    Bars("today-steps-${data.date}", a.hourly.map { it.toFloat() }, height = 40.dp)
                    stepsStreak?.let { Text(it, Modifier.padding(top = Space.s), style = Type.small, color = Palette.Muted) }
                }
            }
            add { m ->
                StatTile(
                    "Recovery", null, null,
                    when (r) {
                        is ReadinessResult.Ready -> bandWord(r.band)
                        is ReadinessResult.Calibrating -> "Night ${r.nights} of ${Tuning.MIN_NIGHTS}"
                        ReadinessResult.NoData -> "no data"
                    },
                    m, icon = NavIcons.today, onClick = metricOpener(MetricRef.Kind(MetricKind.RECOVERY)), description = "Recovery details", tint = Tint.ROSE,
                ) {
                    when (r) {
                        is ReadinessResult.Ready -> CountUpText("recovery-${data.date}", r.score.toLong(), Type.tile.copy(fontSize = 34.sp), color = Palette.Text)
                        is ReadinessResult.Calibrating -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(Tuning.MIN_NIGHTS) { i -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (i < r.nights) Palette.Accent else Palette.Divider)) }
                        }
                        ReadinessResult.NoData -> Text("—", style = Type.tile.copy(fontSize = 34.sp), color = Palette.Faint)
                    }
                }
            }
            h?.hrv?.let { v ->
                add { m ->
                    StatTile(
                        "HRV", v.roundToInt().toString(), "ms", rangeNote(v, h.hrvRange, Good.HIGHER), m,
                        status = h.hrvRange?.let { rangeWord(rangeStatus(v, it, Good.HIGHER)) },
                        icon = NavIcons.activity,
                        onClick = metricOpener(MetricRef.Kind(MetricKind.HRV)),
                        description = "HRV details",
                        tint = Tint.PEACH,
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
                        onClick = metricOpener(MetricRef.Kind(MetricKind.RESTING_HR)),
                        description = "Resting heart rate details",
                        tint = Tint.SAND,
                    ) { h.restingHrRange?.let { RangeBar("today-rhr", v, it, Good.LOWER) } ?: EmptyTrack() }
                }
            }
        }
        revealItem("tiles", 2) { TileGrid(tiles) }
        val hd = data.heartDay?.takeIf { prefs.showHeartRate }
        val workouts = data.workouts.takeIf { prefs.showWorkouts }.orEmpty()
        if (hd != null || workouts.isNotEmpty() || prefs.showWater) revealItem("rows", 3) {
            var expanded by rememberSaveable(data.date) { mutableStateOf(false) }
            var adds by remember { mutableIntStateOf(0) }
            var canUndo by remember { mutableStateOf(false) }
            LaunchedEffect(adds) {
                if (adds > 0) {
                    canUndo = true
                    delay(UNDO_MILLIS)
                    canUndo = false
                }
            }
            Column(Modifier.animateContentSize()) {
                RowGroup {
                    if (hd != null) {
                        ListRow(
                            NavIcons.heart, "Heart rate",
                            h?.restingHr?.let { "${it.roundToInt()} bpm resting" } ?: "latest ${hd.latest.value.roundToInt()} bpm",
                            Modifier.semantics { contentDescription = "Heart rate details" },
                            onClick = metricOpener(MetricRef.Kind(MetricKind.HEART_RATE)),
                            trailing = { HeartDayLine("heart-row-${data.date}", hd.line, Modifier.size(70.dp, 28.dp)) },
                        )
                    }
                    if (workouts.size == 1) {
                        val w = workouts.first()
                        ListRow(workoutIconVector(workoutIcon(w.type)), workoutTitle(w), workoutLine(w, zone, miles, prefs.clock24), Modifier.semantics { contentDescription = "${workoutTitle(w)} details" }, onClick = { onOpenWorkout(w) }, divider = hd != null)
                    }
                    if (workouts.size > 1) {
                        // Several sessions fold into one row: "4 workouts · 78 min · 7.7 km", tap to expand.
                        val km = workouts.mapNotNull { it.distanceMeters }.sum() / 1000
                        val subtitle = listOfNotNull("${activeMinutes(workouts)}m", km.takeIf { it > 0 }?.let { formatKm(it, miles) }).joinToString(" · ")
                        ListRow(NavIcons.flame, "${workouts.size} workouts", subtitle, Modifier.semantics { contentDescription = "${workouts.size} workouts, ${if (expanded) "collapse" else "expand"}" }, onClick = { expanded = !expanded }, divider = hd != null)
                        if (expanded) workouts.forEach { w ->
                            ListRow(workoutIconVector(workoutIcon(w.type)), workoutTitle(w), workoutLine(w, zone, miles, prefs.clock24), Modifier.semantics { contentDescription = "${workoutTitle(w)} details" }, onClick = { onOpenWorkout(w) }, divider = true)
                        }
                    }
                    if (prefs.showWater) {
                        ListRow(
                            NavIcons.drop, "Water",
                            listOfNotNull("${formatLiters(data.waterLiters)} of ${formatLiters(data.waterGoal)} L", waterStreak).joinToString("\n"),
                            divider = hd != null || workouts.isNotEmpty(),
                            trailing = {
                                if (data.isToday && canUndo) {
                                    Text("Undo", Modifier.clip(RoundedCornerShape(8.dp)).tappable({ canUndo = false; onUndoWater() }, "Undo water").padding(Space.s), style = Type.label, color = Palette.Accent)
                                } else if (data.isToday) {
                                    Box(Modifier.size(48.dp).tappable({ adds++; onAddWater(250) }, "Add 250 millilitres"), contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.Accent), contentAlignment = Alignment.Center) {
                                            Text("+", style = Type.body.copy(fontSize = 20.sp), color = Color.White)
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        if (prefs.showSuggestions && data.suggestions.isNotEmpty()) {
            val first = data.suggestions.first()
            revealItem("suggestion-0", 4) {
                StripCard(null, null) {
                    Icon(suggestionIcon(first.kind), null, Modifier.padding(end = Space.m).size(20.dp), tint = Palette.Accent)
                    Text(suggestionText(first), style = Type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp), color = Palette.Text)
                }
            }
            if (data.suggestions.size > 1) revealItem("suggestions-more", 5) {
                RowGroup { data.suggestions.drop(1).forEachIndexed { i, s -> ListRow(suggestionIcon(s.kind), suggestionText(s), null, divider = i > 0) } }
            }
        }
        // Monday: last week in one card, the week-over-week view that otherwise lives on Trends.
        if (data.isToday && data.date.dayOfWeek == java.time.DayOfWeek.MONDAY) {
            weekReview(data.series, data.date.minusDays(1))?.let { review -> item(key = "week-review") { WeekReviewCard(review) } }
        }
        item(key = "pills") {
            Row(Modifier.padding(horizontal = Space.l).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("SHARE DAY", Palette.Text, Palette.Divider, onShareDay, Modifier.weight(1f))
                Pill("ASK AI", Palette.Accent, Palette.Accent.copy(alpha = 0.45f), onAskAi, Modifier.weight(1f), icon = NavIcons.sparkle)
            }
        }
    }
}

// "DATA UP TO 9:43 PM · SYNC": tells the user how fresh the watch data is and opens the wearable app to push more.
// While a read runs, a small ring fills and the row names the record type that just came back.
@Composable
private fun SyncRow(data: TodayData, onSync: (String) -> Unit, refreshing: Boolean, progressFlow: StateFlow<ReadProgress?>, error: Boolean) {
    val progress by progressFlow.collectAsStateWithLifecycle()
    val prefs = LocalPrefs.current
    val now = remember(data.date) { java.time.ZonedDateTime.now() }
    val stale = syncStale(data.latestData, now)
    // A clean read ends on "Up to date" for two seconds before the row goes back to the data time.
    var wasRefreshing by remember { mutableStateOf(refreshing) }
    var justDone by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (wasRefreshing && !refreshing && !error) {
            justDone = true
            delay(2000)
        }
        justDone = false
        wasRefreshing = refreshing
    }
    val phase = if (refreshing) 0 else if (justDone) 1 else 2
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.xl), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(phase, transitionSpec = { fadeIn(tween(Motion.PAGE)) togetherWith fadeOut(tween(Motion.PAGE)) }, label = "sync-state") { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p == 0) MiniRing("sync-read", progress?.fraction ?: 0f, Palette.Accent, Modifier.padding(end = 6.dp), diameter = 12.dp, stroke = 2.dp)
                if (p == 1) Icon(NavIcons.check, null, Modifier.padding(end = 4.dp).size(12.dp), tint = Palette.Accent)
                Text(
                    when (p) {
                        0 -> progress?.let { "${it.label} · ${it.done} of ${it.total}" } ?: "Updating…"
                        1 -> "Up to date"
                        else -> data.latestData?.let { dataUpTo(it, now, prefs.clock24) } ?: "No watch data yet"
                    },
                    style = Type.tiny,
                    color = when (p) { 0 -> Palette.Muted; 1 -> Palette.Accent; else -> if (stale) Palette.Warn else Palette.Faint },
                )
            }
        }
        data.syncApp?.let { pkg ->
            Text(
                "Sync ${sourceAppName(pkg)}",
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

private fun readout(data: TodayData): String = when (val r = data.readiness) {
    is ReadinessResult.Ready -> (if (data.isToday) morningReadout(data.sleep?.asleep, data.heart, null) else null) ?: bandAdvice(r.band)
    is ReadinessResult.Calibrating -> {
        val left = Tuning.MIN_NIGHTS - r.nights
        val usable = listOfNotNull(data.sleep?.let { "sleep" }, data.activity?.let { "steps" }, data.regularity?.let { "sleep regularity" })
        if (!data.isToday) "Not enough earlier data for this day" else listOfNotNull(
            "Your baseline is forming: ${if (left == 1) "1 more night" else "$left more nights"} of sleep, HRV and resting heart rate.",
            if (usable.isEmpty()) null else "Usable now: ${usable.joinToString(", ")}.",
        ).joinToString(" ")
    }
    ReadinessResult.NoData -> "No data for last night. Sync your watch, then pull down to refresh."
}
