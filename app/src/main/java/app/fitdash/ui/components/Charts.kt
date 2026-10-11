package app.fitdash.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.fitdash.health.SleepStage
import app.fitdash.health.StageType
import app.fitdash.ui.formatSteps
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Type
import java.time.Duration
import kotlin.math.max
import kotlin.math.roundToLong

@Composable
fun rememberDrawProgress(durationMillis: Int = 1500): Float {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing)) }
    return p.value
}

@Composable
fun CountUpText(target: Long, style: TextStyle) {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) {
        value.animateTo(target.toFloat(), tween(1600, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
    }
    Text(formatSteps(value.value.roundToLong()), style = style)
}

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
fun Hypnogram(stages: List<SleepStage>, modifier: Modifier = Modifier) {
    val progress = rememberDrawProgress()
    Canvas(modifier.fillMaxWidth().height(72.dp)) {
        if (stages.isEmpty()) return@Canvas
        val start = stages.minOf { it.start }
        val total = Duration.between(start, stages.maxOf { it.end }).seconds.toFloat()
        val lane = size.height / 4
        clipRect(right = size.width * progress) {
            stages.forEach { s ->
                val x0 = Duration.between(start, s.start).seconds / total * size.width
                val x1 = Duration.between(start, s.end).seconds / total * size.width
                drawRoundRect(
                    color = stageColor(s.type),
                    topLeft = Offset(x0, stageRow(s.type) * lane + lane * 0.25f),
                    size = Size(max(x1 - x0, 1f), lane * 0.5f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun TrendLine(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    val progress = rememberDrawProgress()
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val lo = values.min()
        val span = (values.max() - lo).takeIf { it > 0 } ?: 1.0
        val pad = 3.dp.toPx()
        val path = Path().apply {
            values.forEachIndexed { i, v ->
                val x = i * size.width / (values.size - 1)
                val y = pad + (size.height - 2 * pad) * (1 - ((v - lo) / span).toFloat())
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * progress, partial, true)
        drawPath(partial, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun Bars(
    values: List<Float>,
    colorFor: (Int) -> Color,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
) {
    val progress = rememberDrawProgress(900)
    val top = values.maxOrNull()?.takeIf { it > 0 } ?: 1f
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(56.dp)) {
            val w = size.width / values.size
            values.forEachIndexed { i, v ->
                val h = v / top * size.height * progress
                drawRoundRect(
                    color = colorFor(i),
                    topLeft = Offset(i * w + 2.dp.toPx(), size.height - h),
                    size = Size(w - 4.dp.toPx(), h),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                labels.forEach { Text(it, Modifier.weight(1f), style = Type.small, color = Palette.Faint, textAlign = TextAlign.Center) }
            }
        }
    }
}
