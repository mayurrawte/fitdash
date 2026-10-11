package app.fitdash.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.fitdash.metrics.Tuning
import app.fitdash.metrics.TodayData
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.CountUpText
import app.fitdash.ui.components.EmptyState
import app.fitdash.ui.components.EmptyTrack
import app.fitdash.ui.components.MiniRing
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.StatTile
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.components.TrendLine
import app.fitdash.ui.dayLetter
import app.fitdash.ui.formatNumber
import app.fitdash.ui.formatSteps
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun ActivityPage(data: TodayData, chrome: PageChrome) {
    val a = data.activity
    PageColumn(chrome, accent = Palette.Green) {
        if (a == null) {
            item(key = "empty") { EmptyState("No activity yet today", "Sync your Fitbit, then pull down to refresh.") }
        } else {
            pageHeader("TODAY", null, null)
            revealItem("goal", 0) {
                SectionCard("STEPS") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniRing("steps-goal", a.goalPercent / 100f, scoreColor(a.goalPercent), diameter = 104.dp, stroke = 9.dp)
                        Column(Modifier.padding(start = Space.l)) {
                            CountUpText("steps-count", a.steps, Type.title, color = Palette.Text)
                            Text("of ${formatSteps(Tuning.STEP_GOAL.toLong())} steps", style = Type.small, color = Palette.Muted)
                            val left = Tuning.STEP_GOAL - a.steps
                            Text(
                                if (left > 0) "${formatSteps(left)} to go" else "Goal done",
                                style = Type.small,
                                color = scoreColor(a.goalPercent),
                                modifier = Modifier.padding(top = Space.xs),
                            )
                        }
                    }
                    val top = a.hourly.maxOrNull() ?: 0L
                    val green = Palette.Green
                    val dimGreen = Palette.DimGreen
                    Bars(
                        "steps-hourly",
                        a.hourly.map { it.toFloat() },
                        { i -> if (top > 0 && a.hourly[i] * 3 > top * 2) green else dimGreen },
                        Modifier.padding(top = Space.m),
                        height = 46.dp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("00", "06", "12", "18", "24").forEach { Text(it, style = Type.tiny, color = Palette.Faint) }
                    }
                }
            }
            val distance = data.activityWeek.mapNotNull { it.distanceKm }
            val kcal = data.activityWeek.mapNotNull { it.kcal }
            item(key = "activity-tiles") {
                TileGrid(
                    listOf<@Composable (Modifier) -> Unit>(
                        { m ->
                            val left = Tuning.ACTIVE_MINUTES_GOAL - a.activeMinutes
                            StatTile("Active", a.activeMinutes.toString(), "min", if (left <= 0) "goal ${Tuning.ACTIVE_MINUTES_GOAL} min ✓" else "$left min to go", m) {
                                MiniRing("active-ring", a.activeMinutes / Tuning.ACTIVE_MINUTES_GOAL.toFloat(), scoreColor((a.activeMinutes * 100 / Tuning.ACTIVE_MINUTES_GOAL).toInt()))
                            }
                        },
                        { m ->
                            StatTile("Distance", formatNumber(a.distanceKm), "km", if (distance.isEmpty()) "this week" else "7-day avg ${formatNumber(distance.average())} km", m) {
                                if (distance.size >= 2) TrendLine("distance-week", distance, Palette.Hrv, Modifier.fillMaxWidth().height(28.dp)) else EmptyTrack()
                            }
                        },
                        { m ->
                            StatTile("Calories", formatSteps(a.kcal.roundToLong()), "kcal", if (kcal.isEmpty()) "this week" else "7-day avg ${formatSteps(kcal.average().roundToLong())}", m) {
                                if (kcal.size >= 2) TrendLine("kcal-week", kcal, Palette.Amber, Modifier.fillMaxWidth().height(28.dp)) else EmptyTrack()
                            }
                        },
                    ),
                )
            }
        }
        if (data.activityWeek.any { it.steps != null }) {
            val known = data.activityWeek.mapNotNull { it.steps }
            revealItem("steps-week", 1) {
                SectionCard("STEPS · 7 DAYS", trailing = "avg ${formatSteps(known.average().roundToLong())}") {
                    val divider = Palette.Divider
                    val dayColors = data.activityWeek.map { d -> d.steps?.let { scoreColor((it * 100.0 / Tuning.STEP_GOAL).roundToInt()) } ?: divider }
                    Bars(
                        "steps-week",
                        data.activityWeek.map { (it.steps ?: 0L).toFloat() },
                        { i -> dayColors[i] },
                        labels = data.activityWeek.map { dayLetter(it.date) },
                    )
                }
            }
        }
    }
}
