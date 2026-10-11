package app.fitdash.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.sp
import app.fitdash.health.SleepStage
import app.fitdash.health.StageType
import app.fitdash.metrics.Good
import app.fitdash.metrics.Status
import app.fitdash.ui.formatSteps
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import java.time.Duration
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sin

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
    val track = Chart.track
    val band = Palette.Good.copy(alpha = 0.45f)
    val ring = Palette.Background
    val dot = Palette.Text
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val span = (range.endInclusive - range.start).takeIf { it > 0 } ?: max(abs(range.start) * 0.1, 1.0)
        val lo = minOf(range.start, value) - span * 0.6
        val hi = maxOf(range.endInclusive, value) + span * 0.6
        fun x(v: Double) = ((v - lo) / (hi - lo)).toFloat() * size.width
        val cy = size.height / 2
        val th = 3.dp.toPx()
        val r = CornerRadius(th / 2)
        drawRoundRect(track, Offset(0f, cy - th / 2), Size(size.width, th), r)
        drawRoundRect(
            band,
            Offset(x(range.start), cy - th / 2),
            Size(max(x(range.endInclusive) - x(range.start), th), th),
            r,
        )
        val dx = max(x(value) * progress.value, 7.5.dp.toPx())
        drawCircle(ring, 7.5.dp.toPx(), Offset(dx, cy))
        drawCircle(dot, 4.5.dp.toPx(), Offset(dx, cy))
    }
}

@Composable
fun TypicalShareBar(share: Float, typical: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier, key: String = "typical-$share-$typical") {
    val progress = rememberDrawProgress(key, Motion.RING)
    val track = Chart.track
    val fill = Palette.Accent
    val bracket = Palette.Text.copy(alpha = 0.45f)
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val cy = size.height / 2
        val th = 4.dp.toPx()
        val r = CornerRadius(th / 2)
        drawRoundRect(track, Offset(0f, cy - th / 2), Size(size.width, th), r)
        drawRoundRect(fill, Offset(0f, cy - th / 2), Size(size.width * share.coerceIn(0f, 1f) * progress.value, th), r)
        val x0 = size.width * typical.start.coerceIn(0f, 1f)
        val x1 = size.width * typical.endInclusive.coerceIn(0f, 1f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
        drawRoundRect(
            bracket,
            Offset(x0, 0.5.dp.toPx()),
            Size(x1 - x0, size.height - 1.dp.toPx()),
            CornerRadius(2.dp.toPx()),
            style = Stroke(1.dp.toPx(), pathEffect = dash),
        )
    }
}

private const val GAUGE_START = 189f
private const val GAUGE_SWEEP = 162f

@Composable
fun SemiGauge(score: Int, label: String, status: Status, modifier: Modifier = Modifier, key: String = "gauge-$label-$score") {
    val progress = rememberDrawProgress(key, Motion.DRAW)
    val track = Chart.track
    val from = Palette.Deep
    val to = Palette.Rem
    Box(modifier.size(300.dp, 160.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val s = 14.dp.toPx()
            val radius = size.width / 2 - s / 2
            val centre = Offset(size.width / 2, size.height - 15.dp.toPx())
            val topLeft = Offset(centre.x - radius, centre.y - radius)
            val arc = Size(radius * 2, radius * 2)
            drawArc(track, GAUGE_START, GAUGE_SWEEP, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round))
            val sweep = GAUGE_SWEEP * score.coerceIn(0, 100) / 100f * progress.value
            drawArc(
                Brush.horizontalGradient(listOf(from, to), startX = centre.x - radius, endX = centre.x + radius),
                GAUGE_START, sweep, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round),
            )
            val a = Math.toRadians((GAUGE_START + sweep).toDouble())
            drawCircle(to, 7.dp.toPx(), Offset(centre.x + radius * cos(a).toFloat(), centre.y + radius * sin(a).toFloat()))
        }
        Column(Modifier.padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), style = Type.label, color = Palette.Muted)
            Text(score.toString(), style = Type.hero.copy(fontSize = 50.sp), color = Palette.Text)
            Text(
                statusWord(status),
                Modifier.padding(top = 2.dp).clip(RoundedCornerShape(8.dp)).background(Palette.Chip).padding(horizontal = 12.dp, vertical = 5.dp),
                style = Type.tiny.copy(fontSize = 9.5.sp),
                color = Palette.Text,
            )
        }
    }
}

@Composable
fun SpanBar(start: String, end: String, modifier: Modifier = Modifier, from: Float = 0f, to: Float = 1f, key: String = "span-$start-$end") {
    val progress = rememberDrawProgress(key, Motion.RING)
    val track = Chart.track
    val a = Palette.Light
    val b = Palette.Rem
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(4.dp)) {
            val r = CornerRadius(2.dp.toPx())
            drawRoundRect(track, Offset.Zero, size, r)
            val x0 = size.width * from
            val w = size.width * (to - from) * progress.value
            drawRoundRect(Brush.horizontalGradient(listOf(a, b), startX = x0, endX = x0 + size.width * (to - from)), Offset(x0, 0f), Size(w, size.height), r)
        }
        Row(Modifier.fillMaxWidth().padding(top = Space.s), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("\u263E ${start.uppercase()}", style = Type.tiny, color = Palette.Muted)
            Text("\u2600\uFE0E ${end.uppercase()}", style = Type.tiny, color = Palette.Muted)
        }
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
