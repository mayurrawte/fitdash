package app.fitdash.ui.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.fitdash.health.GenericSeries
import app.fitdash.metrics.Good
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.dailyMean
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.EmptyState
import app.fitdash.ui.components.EmptyTrack
import app.fitdash.ui.components.RangeBar
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.SectionLabel
import app.fitdash.ui.components.StatTile
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.components.TrendLine
import app.fitdash.ui.dayLetter
import app.fitdash.ui.formatNumber
import app.fitdash.ui.rangeNote
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.scoreColor
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
fun TrendsPage(data: TodayData, chrome: PageChrome) {
    val h = data.heart
    val hasWeek = data.readinessWeek.any { it.score != null }
    PageColumn(chrome, accent = Palette.Hrv) {
        if (h == null && data.generic.isEmpty() && !hasWeek) {
            item(key = "empty") { EmptyState("No trends yet", "A few days of data from your Fitbit and your trends will build up here.") }
            return@PageColumn
        }
        pageHeader("TRENDS", "Last 14 days", null)
        if (h != null) {
            val tiles = buildList<@Composable (Modifier) -> Unit> {
                h.hrv?.let { v -> add { m -> StatTile("HRV today", v.roundToInt().toString(), "ms", rangeNote(v, h.hrvRange, Good.HIGHER), m) { h.hrvRange?.let { RangeBar("trend-hrv-range", v, it, Good.HIGHER) } ?: EmptyTrack() } } }
                h.restingHr?.let { v -> add { m -> StatTile("Resting HR today", v.roundToInt().toString(), "bpm", rangeNote(v, h.restingHrRange, Good.LOWER), m) { h.restingHrRange?.let { RangeBar("trend-rhr-range", v, it, Good.LOWER) } ?: EmptyTrack() } } }
            }
            if (tiles.isNotEmpty()) item(key = "today-tiles") { TileGrid(tiles) }
            if (h.hrvTrend.size >= 2) {
                revealItem("hrv-trend", 0) {
                    SectionCard("HRV · 14 DAYS", trailing = h.hrvBaseline?.let { "baseline ${it.roundToInt()} ms" }) {
                        TrendLine("hrv-trend", h.hrvTrend, Palette.Hrv, Modifier.fillMaxWidth().height(74.dp), baseline = h.hrvBaseline)
                    }
                }
            }
            if (h.restingHrTrend.size >= 2) {
                revealItem("rhr-trend", 1) {
                    SectionCard("RESTING HR · 14 DAYS", trailing = h.restingHrBaseline?.let { "baseline ${it.roundToInt()} bpm" }) {
                        TrendLine("rhr-trend", h.restingHrTrend, Palette.Rem, Modifier.fillMaxWidth().height(74.dp), baseline = h.restingHrBaseline)
                    }
                }
            }
        }
        if (hasWeek) {
            val known = data.readinessWeek.mapNotNull { it.score }
            revealItem("recovery-week", 2) {
                SectionCard("RECOVERY · 7 DAYS", trailing = "avg ${known.average().roundToInt()}") {
                    Bars(
                        "recovery-week",
                        data.readinessWeek.map { (it.score ?: 0).toFloat() },
                        { i -> data.readinessWeek[i].score?.let(::scoreColor) ?: Palette.Divider },
                        labels = data.readinessWeek.map { dayLetter(it.date) },
                    )
                }
            }
        }
        if (data.generic.isNotEmpty()) {
            item(key = "other-label") { SectionLabel("OTHER DATA") }
            item(key = "other") {
                TileGrid(
                    data.generic.map<GenericSeries, @Composable (Modifier) -> Unit> { g ->
                        { m ->
                            val days = dailyMean(g.points, ZoneId.systemDefault()).toSortedMap().values.toList()
                            StatTile(g.label, formatNumber(days.last()), g.unit, "${days.size} days of data", m) {
                                if (days.size >= 2) TrendLine("generic-${g.label}", days, Palette.Muted, Modifier.fillMaxWidth().height(28.dp)) else EmptyTrack()
                            }
                        }
                    },
                )
            }
        }
    }
}
