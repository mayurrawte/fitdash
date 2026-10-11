package app.fitdash.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.breathMillis
import app.fitdash.ui.theme.scoreColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val OnOrbDark = Color(0xFF0B1A14)

@Composable
fun Orb(score: Int?, modifier: Modifier = Modifier, diameter: Dp = 220.dp, content: @Composable () -> Unit) {
    val target = score?.let(::scoreColor) ?: Palette.Calibrating
    val colour = remember { Animatable(Palette.Calibrating) }
    LaunchedEffect(target) { colour.animateTo(target, tween(Motion.COLOUR, easing = Motion.ease)) }
    val transition = rememberInfiniteTransition(label = "orb")
    val breath = breathMillis(score ?: 50)
    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(breath / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "morph",
    )
    val onOrb = if (colour.value.luminance() > 0.45f) OnOrbDark else Color.White

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }) {
            val c = colour.value
            val highlight = lerp(c, Color.White, 0.55f)
            drawPath(
                blobPath(size, phase, 0.40f),
                Brush.radialGradient(
                    listOf(highlight, c, lerp(c, Color.Black, 0.45f)),
                    center = Offset(size.width * 0.38f, size.height * 0.38f),
                    radius = size.minDimension * 0.55f,
                ),
            )
            drawPath(
                blobPath(size, -phase, 0.34f),
                Brush.radialGradient(
                    listOf(highlight.copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(size.width * 0.6f, size.height * 0.6f),
                    radius = size.minDimension * 0.42f,
                ),
                blendMode = BlendMode.Screen,
            )
        }
        CompositionLocalProvider(LocalContentColor provides onOrb) { content() }
    }
}

private fun blobPath(size: Size, phase: Float, radiusFraction: Float): Path {
    val c = Offset(size.width / 2, size.height / 2)
    val base = size.minDimension * radiusFraction
    val n = 8
    val pts = List(n) { i ->
        val a = i * 2f * PI.toFloat() / n
        val r = base * (1 + 0.06f * sin(phase + i * 1.7f) + 0.04f * cos(2 * phase - i))
        Offset(c.x + r * cos(a), c.y + r * sin(a))
    }
    return Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 0 until n) {
            val p0 = pts[(i - 1 + n) % n]
            val p1 = pts[i]
            val p2 = pts[(i + 1) % n]
            val p3 = pts[(i + 2) % n]
            cubicTo(
                p1.x + (p2.x - p0.x) / 6, p1.y + (p2.y - p0.y) / 6,
                p2.x - (p3.x - p1.x) / 6, p2.y - (p3.y - p1.y) / 6,
                p2.x, p2.y,
            )
        }
        close()
    }
}

@Preview(backgroundColor = 0xFF0A0A0D, showBackground = true)
@Composable
private fun OrbPreview() {
    Orb(78) {}
}
