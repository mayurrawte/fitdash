package app.fitdash.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.metrics.DayValue
import app.fitdash.metrics.Good
import app.fitdash.metrics.MetricKind
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Status
import app.fitdash.metrics.normalRange
import app.fitdash.metrics.rangeStatus
import app.fitdash.metrics.rangeWord
import app.fitdash.metrics.scoreStatus
import app.fitdash.ui.GENERIC_EXPLAIN
import app.fitdash.ui.components.Bars
import app.fitdash.ui.components.SegmentedControl
import app.fitdash.ui.components.StatusText
import app.fitdash.ui.components.TrendLine
import app.fitdash.ui.components.ValueText
import app.fitdash.ui.bmiWord
import app.fitdash.ui.dayLetter
import app.fitdash.ui.formatBmi
import app.fitdash.ui.formatLiters
import app.fitdash.ui.formatNumber
import app.fitdash.ui.formatSteps
import app.fitdash.ui.metricExplain
import app.fitdash.ui.metricName
import app.fitdash.ui.theme.LocalTheme
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.bmiColour
import kotlin.math.roundToInt
import kotlin.math.roundToLong

sealed interface MetricRef {
    data class Kind(val kind: MetricKind) : MetricRef
    data class Generic(val label: String, val unit: String) : MetricRef
}

val LocalOpenMetric = staticCompositionLocalOf<(MetricRef) -> Unit> { {} }

@Composable
fun metricOpener(ref: MetricRef): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val open = LocalOpenMetric.current
    return {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        open(ref)
    }
}

fun metricDescription(ref: MetricRef) = when (ref) {
    is MetricRef.Kind -> "${metricName(ref.kind)} details"
    is MetricRef.Generic -> "${ref.label} details"
}

private const val MILES_PER_KM = 0.621371

private val LINE_KINDS = setOf(MetricKind.RESTING_HR, MetricKind.HRV, MetricKind.SPO2, MetricKind.RESPIRATORY, MetricKind.SKIN_TEMP)

private fun goodFor(kind: MetricKind) = when (kind) {
    MetricKind.RESTING_HR -> Good.LOWER
    MetricKind.HRV, MetricKind.SPO2 -> Good.HIGHER
    else -> Good.MIDDLE
}

private fun unitFor(kind: MetricKind, miles: Boolean) = when (kind) {
    MetricKind.RECOVERY, MetricKind.SLEEP_SCORE, MetricKind.STEPS -> ""
    MetricKind.SLEEP_HOURS -> "h"
    MetricKind.DEEP, MetricKind.REM, MetricKind.LIGHT, MetricKind.AWAKE, MetricKind.ACTIVE_MINUTES -> "min"
    MetricKind.RESTING_HR -> "bpm"
    MetricKind.HRV -> "ms"
    MetricKind.SPO2 -> "%"
    MetricKind.RESPIRATORY -> "br/min"
    MetricKind.SKIN_TEMP -> "°C"
    MetricKind.DISTANCE -> if (miles) "mi" else "km"
    MetricKind.CALORIES -> "kcal"
    MetricKind.WATER -> "L"
    MetricKind.BMI -> ""
}

private fun format(kind: MetricKind?, v: Double) = when (kind) {
    MetricKind.STEPS, MetricKind.CALORIES -> formatSteps(v.roundToLong())
    MetricKind.WATER -> formatLiters(v)
    MetricKind.BMI -> formatBmi(v)
    else -> formatNumber(v)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailSheet(ref: MetricRef, data: TodayData, miles: Boolean, onDismiss: () -> Unit) {
    val kind = (ref as? MetricRef.Kind)?.kind
    val scale = if (kind == MetricKind.DISTANCE && miles) MILES_PER_KM else 1.0
    val series: List<DayValue> = when (ref) {
        is MetricRef.Kind -> data.series[ref.kind].orEmpty()
        is MetricRef.Generic -> data.genericDaily[ref.label].orEmpty()
    }.map { d -> d.copy(value = d.value?.times(scale)) }
    val name = when (ref) {
        is MetricRef.Kind -> metricName(ref.kind)
        is MetricRef.Generic -> ref.label
    }
    val unit = when (ref) {
        is MetricRef.Kind -> unitFor(ref.kind, miles)
        is MetricRef.Generic -> ref.unit
    }
    val explain = kind?.let(::metricExplain) ?: GENERIC_EXPLAIN
    var month by rememberSaveable { mutableStateOf(false) }
    val visible = series.takeLast(if (month) series.size else 7)
    val known = visible.mapNotNull { it.value }
    val selected = if (kind == MetricKind.BMI) data.bmi else series.lastOrNull()?.value
    val range = kind?.takeIf { it in LINE_KINDS }?.let { k ->
        normalRange(series.mapNotNull { d -> d.value?.let { d.date to it } }.toMap(), data.date)?.let { it to goodFor(k) }
    }
    val status: Status? = when {
        selected == null -> null
        kind == MetricKind.RECOVERY || kind == MetricKind.SLEEP_SCORE -> scoreStatus(selected.roundToInt())
        range != null -> rangeWord(rangeStatus(selected, range.first, range.second))
        else -> null
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LocalTheme.current.bgMid) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = Space.xxl)) {
            Text(name.uppercase(), Modifier.padding(horizontal = Space.xl), style = Type.label, color = Palette.Muted)
            if (selected != null) {
                ValueText(format(kind, selected), unit, Modifier.padding(start = Space.xl, end = Space.xl, top = Space.xs), style = Type.hero.copy(fontSize = 56.sp))
            } else {
                Text("—", Modifier.padding(horizontal = Space.xl), style = Type.hero.copy(fontSize = 56.sp), color = Palette.Faint)
            }
            if (status != null) StatusText(status, Modifier.padding(horizontal = Space.xl))
            if (kind == MetricKind.BMI && data.bmiBand != null) {
                Text(bmiWord(data.bmiBand), Modifier.padding(horizontal = Space.xl), style = Type.tiny.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium), color = bmiColour(data.bmiBand))
            }
            Text(explain, Modifier.padding(horizontal = Space.xl, vertical = Space.m), style = Type.body, color = Palette.Muted)
            if (kind != MetricKind.BMI) {
                SegmentedControl(listOf("Week", "Month"), if (month) 1 else 0, { month = it == 1 })
                if (known.size < 2) {
                    Text(
                        "NOT ENOUGH DATA YET",
                        Modifier.padding(horizontal = Space.xl, vertical = Space.xxl),
                        style = Type.label,
                        color = Palette.Faint,
                    )
                } else {
                    val key = "detail-${kind ?: name}-${if (month) "month" else "week"}"
                    val chart = Modifier.fillMaxWidth().padding(horizontal = Space.xl, vertical = Space.l)
                    if (kind == null || kind in LINE_KINDS) {
                        TrendLine(key, known, chart.height(120.dp))
                    } else {
                        Bars(
                            key,
                            visible.map { (it.value ?: 0.0).toFloat() },
                            chart,
                            labels = if (month) emptyList() else visible.map { dayLetter(it.date) },
                            height = 120.dp,
                            poor = { i ->
                                val v = visible[i].value
                                v != null && when (kind) {
                                    MetricKind.RECOVERY, MetricKind.SLEEP_SCORE -> v < 50
                                    MetricKind.STEPS -> v < data.goals.steps * 0.5
                                    else -> false
                                }
                            },
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = Space.xl), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("MIN" to known.min(), "AVG" to known.average(), "MAX" to known.max()).forEach { (label, v) ->
                            Column {
                                Text(label, style = Type.tiny, color = Palette.Muted)
                                Text(format(kind, v), style = Type.tile, color = Palette.Text)
                            }
                        }
                    }
                }
                if (range != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Space.xl, vertical = Space.l), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("NORMAL RANGE", style = Type.tiny, color = Palette.Muted)
                        Text(
                            "${formatNumber(range.first.start)} – ${formatNumber(range.first.endInclusive)} $unit".trim(),
                            style = Type.small,
                            color = Palette.Text,
                        )
                    }
                }
            }
        }
    }
}
