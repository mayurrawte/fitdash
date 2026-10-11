package app.fitdash.ui.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.fitdash.health.GenericSeries
import app.fitdash.metrics.DayTrend
import app.fitdash.metrics.MetricKind
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
import app.fitdash.ui.bmiWord
import app.fitdash.ui.dayLetter
import app.fitdash.ui.daysOfData
import app.fitdash.ui.formatBmi
import app.fitdash.ui.formatNumber
import app.fitdash.ui.formatSteps
import app.fitdash.ui.metricName
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.bmiColour
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
        @Composable fun opener(kind: MetricKind) = metricOpener(MetricRef.Kind(kind))
        fun scoreBars(key: String, label: String, kind: MetricKind, pick: (DayTrend) -> Int?, index: Int) {
            val values = days.map(pick)
            if (values.all { it == null }) return
            revealItem("$key-$range", index) {
                SectionCard(label, trailing = "avg ${values.filterNotNull().average().roundToInt()}", onClick = opener(kind), description = "${metricName(kind)} details") {
                    Bars("$key-$range", values.map { (it ?: 0).toFloat() }, labels = letters, poor = { i -> (values[i] ?: 100) < 50 })
                }
            }
        }
        scoreBars("recovery", "RECOVERY", MetricKind.RECOVERY, { it.recovery }, 0)
        scoreBars("sleep-score", "SLEEP SCORE", MetricKind.SLEEP_SCORE, { it.sleepScore }, 1)
        if (days.any { it.steps != null }) {
            val known = days.mapNotNull { it.steps }
            revealItem("steps-$range", 2) {
                SectionCard("STEPS", trailing = "avg ${formatSteps(known.average().roundToLong())}", onClick = opener(MetricKind.STEPS), description = "Steps details") {
                    Bars(
                        "steps-$range",
                        days.map { (it.steps ?: 0L).toFloat() },
                        labels = letters,
                        poor = { i -> (days[i].steps ?: Long.MAX_VALUE) * 2 < data.goals.steps },
                    )
                }
            }
        }
        val rhr = days.filter { it.restingHr != null }
        if (rhr.size >= 2) {
            val estimated = rhr.any { it.date in data.estimatedRestingHrDates }
            revealItem("rhr-$range", 3) {
                SectionCard("RESTING HR", trailing = "avg ${rhr.mapNotNull { it.restingHr }.average().roundToInt()} bpm" + if (estimated) " · est." else "", onClick = opener(MetricKind.RESTING_HR), description = "Resting heart rate details") {
                    TrendLine("rhr-$range", rhr.mapNotNull { it.restingHr }, Modifier.fillMaxWidth().height(74.dp))
                }
            }
        }
        val hrv = days.mapNotNull { it.hrv }
        if (hrv.size >= 2) {
            revealItem("hrv-$range", 4) {
                SectionCard("HRV", trailing = "avg ${hrv.average().roundToInt()} ms", onClick = opener(MetricKind.HRV), description = "HRV details") {
                    TrendLine("hrv-$range", hrv, Modifier.fillMaxWidth().height(74.dp))
                }
            }
        }
        item(key = "bmi") {
            val open = opener(MetricKind.BMI)
            TileGrid(listOf { m ->
                val bmi = data.bmi
                val band = data.bmiBand
                if (bmi == null || band == null) {
                    StatTile("BMI", "—", null, "Add height in Settings", m, onClick = chrome.onMenu, description = "Add height in Settings") { EmptyTrack() }
                } else {
                    StatTile("BMI", formatBmi(bmi), null, "general guide, not medical advice", m, onClick = open, description = "BMI details") {
                        Text(bmiWord(band), style = Type.tiny.copy(fontWeight = FontWeight.Medium), color = bmiColour(band))
                    }
                }
            })
        }
        if (data.generic.isNotEmpty()) {
            item(key = "other-label") { SectionLabel("OTHER DATA") }
            item(key = "other") {
                TileGrid(
                    data.generic.map<GenericSeries, @Composable (Modifier) -> Unit> { g ->
                        { m ->
                            val daily = dailyMean(g.points, ZoneId.systemDefault()).toSortedMap().values.toList()
                            val ref = MetricRef.Generic(g.label, g.unit)
                            StatTile(g.label, formatNumber(daily.last()), g.unit, daysOfData(daily.size), m, onClick = metricOpener(ref), description = metricDescription(ref)) {
                                if (daily.size >= 2) TrendLine("generic-${g.label}", daily, Modifier.fillMaxWidth().height(28.dp)) else EmptyTrack()
                            }
                        }
                    },
                )
            }
        }
    }
}
