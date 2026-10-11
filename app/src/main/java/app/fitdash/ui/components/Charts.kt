package app.fitdash.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.fitdash.health.SleepStage
import app.fitdash.health.StageType
import app.fitdash.metrics.Good
import app.fitdash.metrics.RangeStatus
import app.fitdash.metrics.rangeStatus
import app.fitdash.ui.formatSteps
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

object Chart {
    val stroke = 2.dp
    val corner = 3.dp
    val track: Color @Composable @ReadOnlyComposable get() = Palette.Divider
}

val LocalRevealed = staticCompositionLocalOf<MutableSet<String>> { mutableSetOf() }

@Composable
fun rememberDrawProgress(key: String, durationMillis: Int = Motion.DRAW, delayMillis: Int = 0): State<Float> {
    val revealed = LocalRevealed.current
    val p = remember(key) { Animatable(if (key in revealed) 1f else 0f) }
    LaunchedEffect(key) {
        if (p.value < 1f) {
            p.animateTo(1f, tween(durationMillis, delayMillis, Motion.ease))
            revealed += key
        }
    }
    return p.asState()
}

@Composable
fun CountUpText(key: String, target: Long, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val revealed = LocalRevealed.current
    val value = remember(key) { Animatable(if (key in revealed) target.toFloat() else 0f) }
    LaunchedEffect(key, target) {
        value.animateTo(target.toFloat(), tween(Motion.COUNT, easing = Motion.ease))
        revealed += key
    }
    Text(formatSteps(value.value.roundToLong()), modifier, style = style, color = color)
}

@Composable
@ReadOnlyComposable
private fun stageColor(t: StageType) = when (t) {
    StageType.AWAKE -> Palette.Awake
    StageType.REM -> Palette.Rem
    StageType.LIGHT, StageType.SLEEPING -> Palette.Light
    StageType.DEEP -> Palette.Deep
}

private fun stageRow(t: StageType) = when (t) {
    StageType.AWAKE -> 0
    StageType.REM -> 1
    StageType.LIGHT, StageType.SLEEPING -> 2
    StageType.DEEP -> 3
}

@Composable
fun Hypnogram(key: String, stages: List<SleepStage>, modifier: Modifier = Modifier) {
    val progress = rememberDrawProgress(key)
    val colors = StageType.entries.associateWith { stageColor(it) }
    Canvas(modifier.fillMaxWidth().height(72.dp)) {
        if (stages.isEmpty()) return@Canvas
        val start = stages.minOf { it.start }
        val total = Duration.between(start, stages.maxOf { it.end }).seconds.toFloat()
        if (total <= 0f) return@Canvas
        val lane = size.height / 4
        clipRect(right = size.width * progress.value) {
            stages.forEach { s ->
                val x0 = Duration.between(start, s.start).seconds / total * size.width
                val x1 = Duration.between(start, s.end).seconds / total * size.width
                drawRoundRect(
                    color = colors.getValue(s.type),
                    topLeft = Offset(x0, stageRow(s.type) * lane + lane * 0.25f),
                    size = Size(max(x1 - x0, 1f), lane * 0.5f),
                    cornerRadius = CornerRadius(Chart.corner.toPx()),
                )
            }
        }
    }
}

@Composable
fun TrendLine(key: String, values: List<Double>, color: Color, modifier: Modifier = Modifier, baseline: Double? = null) {
    if (values.size < 2) {
        Spacer(modifier)
        return
    }
    val progress = rememberDrawProgress(key)
    val faint = Palette.Faint
    Spacer(
        modifier.drawWithCache {
            val all = values + listOfNotNull(baseline)
            val lo = all.min()
            val span = (all.max() - lo).takeIf { it > 0 } ?: 1.0
            val pad = 3.dp.toPx()
            fun y(v: Double) = pad + (size.height - 2 * pad) * (1 - ((v - lo) / span).toFloat())
            val path = Path().apply {
                values.forEachIndexed { i, v ->
                    val x = i * size.width / (values.size - 1)
                    if (i == 0) moveTo(x, y(v)) else lineTo(x, y(v))
                }
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            val stroke = Stroke(Chart.stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            onDrawBehind {
                baseline?.let {
                    drawLine(faint, Offset(0f, y(it)), Offset(size.width, y(it)), 1.dp.toPx(), pathEffect = dash)
                }
                partial.reset()
                measure.getSegment(0f, measure.length * progress.value, partial, true)
                drawPath(partial, color, style = stroke)
            }
        },
    )
}

@Composable
fun Bars(
    key: String,
    values: List<Float>,
    colorFor: (Int) -> Color,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    height: Dp = 56.dp,
) {
    val progress = rememberDrawProgress(key, Motion.RISE + 300)
    val top = values.maxOrNull()?.takeIf { it > 0 } ?: 1f
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas
            val w = size.width / values.size
            val gap = (if (values.size > 12) 1.dp else 3.dp).toPx()
            values.forEachIndexed { i, v ->
                val h = v / top * size.height * progress.value
                if (h > 0f) {
                    drawRoundRect(colorFor(i), Offset(i * w + gap, size.height - h), Size(w - 2 * gap, h), CornerRadius(Chart.corner.toPx()))
                }
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = Space.xs)) {
                labels.forEach { Text(it, Modifier.weight(1f), style = Type.tiny, color = Palette.Faint, textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
fun RangeBar(key: String, value: Double, range: ClosedFloatingPointRange<Double>, good: Good, modifier: Modifier = Modifier) {
    val progress = rememberDrawProgress(key, Motion.RING)
    val dot = if (rangeStatus(value, range, good) == RangeStatus.BAD_SIDE) Palette.Amber else Palette.Green
    val track = Chart.track
    val inRange = Palette.Green.copy(alpha = 0.25f)
    val knob = Palette.Card
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val span = (range.endInclusive - range.start).takeIf { it > 0 } ?: max(abs(range.start) * 0.1, 1.0)
        val lo = minOf(range.start, value) - span * 0.6
        val hi = maxOf(range.endInclusive, value) + span * 0.6
        fun x(v: Double) = ((v - lo) / (hi - lo)).toFloat() * size.width
        val cy = size.height / 2
        val th = 8.dp.toPx()
        val r = CornerRadius(th / 2)
        drawRoundRect(track, Offset(0f, cy - th / 2), Size(size.width, th), r)
        drawRoundRect(
            inRange,
            Offset(x(range.start), cy - th / 2),
            Size(max(x(range.endInclusive) - x(range.start), th), th),
            r,
        )
        val dx = max(x(value) * progress.value, 7.dp.toPx())
        drawCircle(knob, 7.dp.toPx(), Offset(dx, cy))
        drawCircle(dot, 5.dp.toPx(), Offset(dx, cy))
    }
}

@Composable
fun MiniRing(
    key: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 38.dp,
    stroke: Dp = 4.dp,
    delayMillis: Int = 0,
) {
    val progress = rememberDrawProgress(key, Motion.RING, delayMillis)
    val track = Chart.track
    Canvas(modifier.size(diameter)) {
        val s = stroke.toPx()
        val topLeft = Offset(s / 2, s / 2)
        val arc = Size(size.width - s, size.height - s)
        drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(s))
        drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f) * progress.value, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round))
    }
}

@Composable
fun ScoreRing(key: String, label: String, score: Int?, index: Int, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier
            .clip(RoundedCornerShape(Space.m))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(Space.s)
            .clearAndSetSemantics { contentDescription = "$label ${score ?: "no data"}"; role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MiniRing(
            key,
            (score ?: 0) / 100f,
            score?.let { scoreColor(it) } ?: Palette.Calibrating,
            diameter = 48.dp,
            stroke = 5.dp,
            delayMillis = index * Motion.STAGGER,
        )
        Spacer(Modifier.height(Space.xs))
        Text("$label ${score ?: "—"}", style = Type.small, color = Palette.Muted)
    }
}
