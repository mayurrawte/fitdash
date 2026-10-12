package app.fitdash.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.health.Workout
import app.fitdash.metrics.paceSecondsPerKm
import app.fitdash.metrics.workoutIcon
import app.fitdash.ui.components.GapLine
import app.fitdash.ui.components.NumberTile
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.components.workoutIconVector
import app.fitdash.ui.formatDuration
import app.fitdash.ui.formatNumber
import app.fitdash.ui.formatPace
import app.fitdash.ui.formatSteps
import app.fitdash.ui.clock
import app.fitdash.ui.sourceAppName
import app.fitdash.ui.theme.LocalTheme
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.workoutTitle
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val MIN_SESSION_HR_SAMPLES = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSheet(workout: Workout, line: List<Double>, miles: Boolean, onDismiss: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val title = workoutTitle(workout)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LocalTheme.current.bgMid) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = Space.xxl)) {
            Row(Modifier.padding(horizontal = Space.xl), verticalAlignment = Alignment.CenterVertically) {
                Icon(workoutIconVector(workoutIcon(workout.type)), null, Modifier.padding(end = Space.s).size(18.dp), tint = Palette.Accent)
                Text(title.uppercase(), style = Type.label, color = Palette.Muted)
            }
            Text(
                formatDuration(workout.duration),
                Modifier.padding(start = Space.xl, end = Space.xl, top = Space.xs),
                style = Type.hero.copy(fontSize = 56.sp),
                color = Palette.Text,
            )
            Text(
                "${clock(workout.start, zone)} – ${clock(workout.end, zone)}".uppercase() +
                    (workout.app.takeIf { it.isNotEmpty() }?.let { " · VIA ${sourceAppName(it).uppercase()}" } ?: ""),
                Modifier.padding(horizontal = Space.xl, vertical = Space.xs),
                style = Type.tiny,
                color = Palette.Muted,
            )
            if (line.size >= MIN_SESSION_HR_SAMPLES) {
                SectionCard("HEART RATE", Modifier.padding(top = Space.l), trailing = workout.avgHr?.let { "avg ${it.roundToInt()} · max ${workout.maxHr?.roundToInt() ?: "—"} bpm" }) {
                    GapLine("workout-hr-${workout.start}", line, Modifier.fillMaxWidth().height(90.dp))
                }
            }
            val tiles = buildList<@Composable (Modifier) -> Unit> {
                workout.distanceMeters?.let { d ->
                    val km = d / 1000
                    add { m -> NumberTile("Distance", NavIcons.walk, listOf((if (miles) formatNumber(km * 0.621371) else formatNumber(km)) to if (miles) "mi" else "km"), m) }
                }
                paceSecondsPerKm(workout)?.let { p -> add { m -> NumberTile("Pace", NavIcons.activity, listOf(formatPace(p, miles) to ""), m) } }
                workout.kcal?.let { k -> add { m -> NumberTile("Calories", NavIcons.flame, listOf(formatSteps(k.roundToLong()) to "kcal"), m) } }
                workout.steps?.let { st -> add { m -> NumberTile("Steps", NavIcons.walk, listOf(formatSteps(st) to ""), m) } }
                workout.avgHr?.let { hr -> add { m -> NumberTile("Avg HR", NavIcons.heart, listOf(hr.roundToInt().toString() to "bpm"), m) } }
                workout.maxHr?.let { hr -> add { m -> NumberTile("Max HR", NavIcons.heart, listOf(hr.roundToInt().toString() to "bpm"), m) } }
            }
            if (tiles.isNotEmpty()) {
                Column(Modifier.padding(top = Space.m)) { TileGrid(tiles) }
            } else {
                Text(
                    "This session has no distance, calories or heart rate attached.",
                    Modifier.padding(horizontal = Space.xl, vertical = Space.l),
                    style = Type.body,
                    color = Palette.Muted,
                )
            }
        }
    }
}
