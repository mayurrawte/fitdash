package app.fitdash.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.health.GenericSeries
import app.fitdash.health.SourceStatus
import app.fitdash.metrics.ActivitySummary
import app.fitdash.metrics.CardKind
import app.fitdash.metrics.HeartSummary
import app.fitdash.metrics.HeroMode
import app.fitdash.metrics.ReadinessResult
import app.fitdash.metrics.SleepSummary
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
import app.fitdash.metrics.VitalReading
import app.fitdash.metrics.WeekDay
import app.fitdash.ui.bandWord
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.Hypnogram
import app.fitdash.ui.components.TrendLine
import app.fitdash.ui.formatDuration
import app.fitdash.ui.formatNumber
import app.fitdash.ui.formatSteps
import app.fitdash.ui.sourceLine
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import java.time.format.TextStyle as DayStyle

@Composable
fun TodayCard(kind: CardKind, data: TodayData) {
    when (kind) {
        CardKind.SLEEP -> data.sleep?.let { SleepCard(it) }
        CardKind.HEART -> data.heart?.let { HeartCard(it) }
        CardKind.VITALS -> VitalsCard(data.vitals)
        CardKind.ACTIVITY -> data.activity?.let { ActivityCard(it, today = data.hero == HeroMode.ACTIVITY) }
        CardKind.READINESS_COMPACT -> (data.readiness as? ReadinessResult.Ready)?.let { ReadinessCompactCard(it) }
        CardKind.WEEK -> WeekCard(data.week)
        CardKind.GENERIC -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { data.generic.forEach { GenericCard(it) } }
        CardKind.SOURCES -> SourcesCard(data.sources)
    }
}

@Composable
private fun SectionCard(
    label: String,
    trailing: String? = null,
    trailingColor: Color = Palette.Muted,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Card)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = Type.label, color = Palette.Muted)
            if (trailing != null) Text(trailing, style = Type.small, color = trailingColor)
        }
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun ValueText(value: String, unit: String? = null, color: Color = Palette.Text) {
    Text(
        buildAnnotatedString {
            append(value)
            if (unit != null) withStyle(SpanStyle(fontSize = 13.sp)) { append(" $unit") }
        },
        style = Type.value,
        color = color,
    )
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = Type.small, color = Palette.Text)
        Text(label, style = Type.small, color = Palette.Muted)
    }
}

@Composable
private fun SleepCard(s: SleepSummary) {
    SectionCard("SLEEP", "score ${s.score}", scoreColor(s.score)) {
        ValueText(formatDuration(s.asleep))
        if (s.hasStages) {
            Hypnogram(s.stages, Modifier.padding(top = 10.dp))
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(formatDuration(s.deep), "deep")
                Stat(formatDuration(s.rem), "REM")
                Stat(formatDuration(s.light), "light")
                Stat(formatDuration(s.awake), "awake")
            }
        } else {
            LinearProgressIndicator(
                progress = { (s.asleep.toMinutes() / (Tuning.SLEEP_TARGET_HOURS * 60)).toFloat().coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = scoreColor(s.score),
                trackColor = Palette.Divider,
            )
        }
    }
}

@Composable
private fun HeartCard(h: HeartSummary) {
    val delta = if (h.hrv != null && h.hrvBaseline != null) h.hrv - h.hrvBaseline else null
    SectionCard(
        "HEART",
        delta?.let { "HRV ${if (it >= 0) "↑" else "↓"} ${abs(it).roundToInt()} vs baseline" },
        if ((delta ?: 0.0) >= 0) scoreColor(80) else scoreColor(30),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            h.hrv?.let { Column { ValueText(it.roundToInt().toString(), "ms"); Text("HRV", style = Type.label, color = Palette.Muted) } }
            h.restingHr?.let {
                Column {
                    ValueText(it.roundToInt().toString(), "bpm")
                    Text("RESTING", style = Type.label, color = Palette.Muted)
                    if (h.restingHrBaseline != null) {
                        val d = it - h.restingHrBaseline
                        Text("${if (d > 0) "↑" else "↓"} ${abs(d).roundToInt()} vs baseline", style = Type.small, color = if (d <= 0) scoreColor(80) else scoreColor(30))
                    }
                }
            }
        }
        if (h.hrvTrend.size >= 2) TrendLine(h.hrvTrend, Palette.Hrv, Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp))
    }
}

@Composable
private fun VitalsCard(vitals: List<VitalReading>) {
    SectionCard("OVERNIGHT VITALS") {
        vitals.forEach { v ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(v.label, style = Type.body)
                Column(horizontalAlignment = Alignment.End) {
                    Text("${formatNumber(v.value)} ${v.unit}", style = Type.body)
                    v.baseline?.let { Text("baseline ${formatNumber(it)}", style = Type.small, color = Palette.Muted) }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(a: ActivitySummary, today: Boolean) {
    SectionCard(if (today) "ACTIVITY TODAY" else "ACTIVITY YESTERDAY", "${a.goalPercent}% of goal", scoreColor(a.goalPercent)) {
        ValueText(formatSteps(a.steps), "steps")
        val top = a.hourly.maxOrNull() ?: 0L
        Bars(
            a.hourly.map { it.toFloat() },
            { i -> if (top > 0 && a.hourly[i] * 3 > top * 2) Palette.Green else Palette.DimGreen },
            Modifier.padding(top = 10.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("${a.activeMinutes} min", "active")
            Stat("${formatNumber(a.distanceKm)} km", "distance")
            Stat(formatSteps(a.kcal.roundToLong()), "kcal")
        }
    }
}

@Composable
private fun ReadinessCompactCard(r: ReadinessResult.Ready) {
    SectionCard("THIS MORNING'S READINESS", bandWord(r.band), scoreColor(r.score)) {
        ValueText(r.score.toString(), color = scoreColor(r.score))
    }
}

@Composable
private fun WeekCard(week: List<WeekDay>) {
    val scored = week.mapNotNull { it.score }
    SectionCard("READINESS · 7 DAYS", if (scored.isEmpty()) null else "avg ${scored.average().roundToInt()}") {
        Bars(
            week.map { (it.score ?: 0).toFloat() },
            { i -> week[i].score?.let(::scoreColor) ?: Palette.Divider },
            Modifier.padding(top = 10.dp),
            labels = week.map { it.date.dayOfWeek.getDisplayName(DayStyle.NARROW, Locale.ENGLISH) },
        )
    }
}

@Composable
private fun GenericCard(series: GenericSeries) {
    SectionCard(series.label.uppercase()) {
        ValueText(formatNumber(series.points.last().value), series.unit)
        if (series.points.size >= 2) {
            TrendLine(series.points.map { it.value }, Palette.Muted, Modifier.fillMaxWidth().height(36.dp).padding(top = 8.dp))
        }
    }
}

@Composable
private fun SourcesCard(sources: List<SourceStatus>) {
    SectionCard("DATA SOURCES") {
        sources.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.label, style = Type.small, color = Palette.Text)
                Text(sourceLine(s), style = Type.small, color = if (s.hasData) Palette.Green else Palette.Faint)
            }
        }
    }
}
