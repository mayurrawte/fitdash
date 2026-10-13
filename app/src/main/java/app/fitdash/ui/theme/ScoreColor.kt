package app.fitdash.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import app.fitdash.metrics.BmiBand
import kotlin.math.roundToInt

fun scoreColor(score: Int, bad: Color, warn: Color, good: Color): Color {
    val s = score.coerceIn(0, 100)
    val (lo, hi, t) = if (s <= 50) Triple(bad, warn, s / 50f) else Triple(warn, good, (s - 50) / 50f)
    fun mix(a: Float, b: Float) = (((a + (b - a) * t) * 255).roundToInt())
    return Color(mix(lo.red, hi.red), mix(lo.green, hi.green), mix(lo.blue, hi.blue))
}

// Soft steps between three clear pastels: a blend of its dark text greens and ambers turns olive on white.
@Composable
@ReadOnlyComposable
fun scoreColor(score: Int): Color = LocalTheme.current.let {
    if (it.soft) when {
        score >= 70 -> Color(0xFF8DB85A)
        score >= 50 -> Color(0xFFE3A06F)
        else -> Color(0xFFD9604F)
    } else scoreColor(score, it.bad, it.warn, it.good)
}

@Composable
@ReadOnlyComposable
fun bmiColour(band: BmiBand): Color = LocalTheme.current.let {
    when (band) {
        BmiBand.UNDER, BmiBand.OVER -> it.warn
        BmiBand.HEALTHY -> it.good
        BmiBand.OBESE -> it.bad
    }
}

fun breathMillis(score: Int): Int = (7000 - (score.coerceIn(15, 98) - 15) / 83.0 * 3000).roundToInt()
