package app.fitdash.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.fitdash.metrics.MetricKind
import app.fitdash.metrics.Status
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
import app.fitdash.metrics.scoreStatus
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.CardShape
import app.fitdash.ui.components.tappable
import app.fitdash.ui.components.EmptyState
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.NumberTile
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.SemiGauge
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.dayLetter
import app.fitdash.ui.formatNumber
import app.fitdash.ui.formatSteps
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val MILES_PER_KM = 0.621371

private fun againstAverage(value: Double, average: Double?): Status? =
    average?.takeIf { it > 0 }?.let { scoreStatus((value / it * 100).roundToInt().coerceAtMost(100)) }

@Composable
fun ActivityPage(data: TodayData, chrome: PageChrome, miles: Boolean) {
    val a = data.activity
    @Composable fun opener(kind: MetricKind) = metricOpener(MetricRef.Kind(kind))
    PageColumn(chrome, accent = Palette.Green) {
        if (a == null) {
            item(key = "empty") { EmptyState(if (data.isToday) "No activity yet today" else "No activity recorded", "Sync your Fitbit, then pull down to refresh.") }
        } else {
            item(key = "gauge") {
                val percent = a.goalPercent.coerceIn(0, 100)
                Column(Modifier.fillMaxWidth().clip(CardShape).tappable(opener(MetricKind.STEPS), "Steps details"), horizontalAlignment = Alignment.CenterHorizontally) {
                    SemiGauge(percent, "Steps", scoreStatus(percent), value = formatSteps(a.steps), tag = "of ${formatSteps(data.goals.steps.toLong())}")
                }
            }
            if (a.hourly.sum() > 0L) revealItem("hourly", 0) {
                SectionCard("STEPS BY HOUR") {
                    Bars(
                        "steps-hourly",
                        a.hourly.map { it.toFloat() },
                        height = 46.dp,
                        highlightLast = false,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("00", "06", "12", "18", "24").forEach { Text(it, style = Type.tiny, color = Palette.Faint) }
                    }
                }
            }
            val distance = data.activityWeek.mapNotNull { it.distanceKm }
            val kcal = data.activityWeek.mapNotNull { it.kcal }
            val unitScale = if (miles) MILES_PER_KM else 1.0
            item(key = "activity-tiles") {
                TileGrid(
                    listOf<@Composable (Modifier) -> Unit>(
                        { m ->
                            val left = Tuning.ACTIVE_MINUTES_GOAL - a.activeMinutes
                            NumberTile(
                                "Active", NavIcons.bolt, listOf(a.activeMinutes.toString() to "min"), m,
                                scoreStatus((a.activeMinutes * 100 / Tuning.ACTIVE_MINUTES_GOAL).toInt().coerceAtMost(100)),
                                if (left <= 0) "GOAL ${Tuning.ACTIVE_MINUTES_GOAL} MIN DONE" else "$left MIN TO GO",
                                opener(MetricKind.ACTIVE_MINUTES), "Active minutes details",
                            )
                        },
                        { m ->
                            NumberTile(
                                "Distance", NavIcons.walk, listOf(formatNumber(a.distanceKm * unitScale) to if (miles) "mi" else "km"), m,
                                againstAverage(a.distanceKm, distance.takeIf { it.isNotEmpty() }?.average()),
                                if (distance.isEmpty()) "THIS WEEK" else "7-DAY AVG ${formatNumber(distance.average() * unitScale)} ${if (miles) "MI" else "KM"}",
                                opener(MetricKind.DISTANCE), "Distance details",
                            )
                        },
                        { m ->
                            NumberTile(
                                "Calories", NavIcons.heart, listOf(formatSteps(a.kcal.roundToLong()) to "kcal"), m,
                                againstAverage(a.kcal, kcal.takeIf { it.isNotEmpty() }?.average()),
                                if (kcal.isEmpty()) "THIS WEEK" else "7-DAY AVG ${formatSteps(kcal.average().roundToLong())}",
                                opener(MetricKind.CALORIES), "Calories details",
                            )
                        },
                    ),
                )
            }
        }
        if (data.activityWeek.any { it.steps != null }) {
            val known = data.activityWeek.mapNotNull { it.steps }
            revealItem("steps-week", 1) {
                SectionCard(
                    "STEPS · 7 DAYS",
                    trailing = "avg ${formatSteps(known.average().roundToLong())}",
                    onClick = opener(MetricKind.STEPS),
                    description = "Steps details",
                ) {
                    Bars(
                        "steps-week",
                        data.activityWeek.map { (it.steps ?: 0L).toFloat() },
                        poor = { i -> (data.activityWeek[i].steps ?: Long.MAX_VALUE) * 2 < data.goals.steps },
                        labels = data.activityWeek.map { dayLetter(it.date) },
                    )
                }
            }
        }
    }
}
