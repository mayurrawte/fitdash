package app.fitdash.ui.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.fitdash.health.GenericSeries
import app.fitdash.metrics.DayTrend
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.dailyMean
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.EmptyState
import app.fitdash.ui.components.EmptyTrack
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.SectionLabel
import app.fitdash.ui.components.SegmentedControl
import app.fitdash.ui.components.StatTile
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.components.TrendLine
import app.fitdash.ui.dayLetter
import app.fitdash.ui.daysOfData
import app.fitdash.ui.formatNumber
import app.fitdash.ui.formatSteps
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.scoreColor
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun TrendsPage(data: TodayData, chrome: PageChrome) {
    var month by rememberSaveable { mutableStateOf(false) }
    val range = if (month) "month" else "week"
    val days = data.trend.takeLast(if (month) data.trend.size else 7)
    val hasTrend = data.trend.any { it.recovery != null || it.sleepScore != null || it.steps != null || it.restingHr != null || it.hrv != null }
    PageColumn(chrome, accent = Palette.Hrv) {
        if (!hasTrend && data.generic.isEmpty()) {
            item(key = "empty") { EmptyState("No trends yet", "A few days of data from your Fitbit and your trends will build up here.") }
            return@PageColumn
        }
        item(key = "toggle") { SegmentedControl(listOf("Week", "Month"), if (month) 1 else 0, { month = it == 1 }) }
        val letters = if (month) emptyList() else days.map { dayLetter(it.date) }
        fun scoreBars(key: String, label: String, pick: (DayTrend) -> Int?, index: Int) {
            val values = days.map(pick)
            if (values.all { it == null }) return
            revealItem("$key-$range", index) {
                SectionCard(label, trailing = "avg ${values.filterNotNull().average().roundToInt()}") {
                    val divider = Palette.Divider
                    val colors = values.map { v -> v?.let { scoreColor(it) } ?: divider }
                    Bars("$key-$range", values.map { (it ?: 0).toFloat() }, { i -> colors[i] }, labels = letters)
                }
            }
        }
        scoreBars("recovery", "RECOVERY", { it.recovery }, 0)
        scoreBars("sleep-score", "SLEEP SCORE", { it.sleepScore }, 1)
        if (days.any { it.steps != null }) {
            val known = days.mapNotNull { it.steps }
            revealItem("steps-$range", 2) {
                SectionCard("STEPS", trailing = "avg ${formatSteps(known.average().roundToLong())}") {
                    val divider = Palette.Divider
                    val colors = days.map { d -> d.steps?.let { scoreColor((it * 100.0 / data.goals.steps).roundToInt()) } ?: divider }
                    Bars("steps-$range", days.map { (it.steps ?: 0L).toFloat() }, { i -> colors[i] }, labels = letters)
                }
            }
        }
        val rhr = days.filter { it.restingHr != null }
        if (rhr.size >= 2) {
            val estimated = rhr.any { it.date in data.estimatedRestingHrDates }
            revealItem("rhr-$range", 3) {
                SectionCard("RESTING HR", trailing = "avg ${rhr.mapNotNull { it.restingHr }.average().roundToInt()} bpm" + if (estimated) " · est." else "") {
                    TrendLine("rhr-$range", rhr.mapNotNull { it.restingHr }, Palette.Rem, Modifier.fillMaxWidth().height(74.dp))
                }
            }
        }
        val hrv = days.mapNotNull { it.hrv }
        if (hrv.size >= 2) {
            revealItem("hrv-$range", 4) {
                SectionCard("HRV", trailing = "avg ${hrv.average().roundToInt()} ms") {
                    TrendLine("hrv-$range", hrv, Palette.Hrv, Modifier.fillMaxWidth().height(74.dp))
                }
            }
        }
        if (data.generic.isNotEmpty()) {
            item(key = "other-label") { SectionLabel("OTHER DATA") }
            item(key = "other") {
                TileGrid(
                    data.generic.map<GenericSeries, @Composable (Modifier) -> Unit> { g ->
                        { m ->
                            val daily = dailyMean(g.points, ZoneId.systemDefault()).toSortedMap().values.toList()
                            StatTile(g.label, formatNumber(daily.last()), g.unit, daysOfData(daily.size), m) {
                                if (daily.size >= 2) TrendLine("generic-${g.label}", daily, Palette.Muted, Modifier.fillMaxWidth().height(28.dp)) else EmptyTrack()
                            }
                        }
                    },
                )
            }
        }
    }
}
