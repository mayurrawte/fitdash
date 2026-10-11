package app.fitdash.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

private data class Stop(val at: Int, val r: Int, val g: Int, val b: Int)

private val STOPS = listOf(Stop(0, 229, 72, 77), Stop(50, 245, 176, 65), Stop(100, 61, 220, 151))

fun scoreColor(score: Int): Color {
    val s = score.coerceIn(0, 100)
    val (lo, hi) = if (s <= 50) STOPS[0] to STOPS[1] else STOPS[1] to STOPS[2]
    val t = (s - lo.at) / (hi.at - lo.at).toFloat()
    fun mix(a: Int, b: Int) = (a + (b - a) * t).roundToInt()
    return Color(mix(lo.r, hi.r), mix(lo.g, hi.g), mix(lo.b, hi.b))
}

fun breathMillis(score: Int): Int = (7000 - (score.coerceIn(15, 98) - 15) / 83.0 * 3000).roundToInt()
