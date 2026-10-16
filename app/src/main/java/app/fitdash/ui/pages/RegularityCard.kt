package app.fitdash.ui.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.fitdash.metrics.FitnessCard
import app.fitdash.metrics.MIN_PAIRS
import app.fitdash.metrics.NightSpan
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.regularityWordBand
import app.fitdash.ui.components.Chart
import app.fitdash.ui.components.CountUpText
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.regularityWord
import app.fitdash.ui.theme.LocalReduceMotion
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import kotlinx.coroutines.delay
import java.time.ZoneId

// Three colours only; regularity never shows red.
@Composable
private fun bandColour(band: Int): Color = when (band) {
    0 -> Palette.Good
    1 -> Palette.Accent
    else -> Palette.Warn
}

private const val AXIS_START_MIN = 20 * 60        // 20:00 the evening before
private const val AXIS_SPAN_MIN = 16 * 60         // to 12:00

@Composable
fun RegularityCard(data: TodayData, zone: ZoneId) {
    val r = data.regularity
    if (r == null) {
        SectionCard("REGULARITY", trailing = "last 14 nights") {
            Text("Calibrating · ${data.regularityProgress} of $MIN_PAIRS nights", style = Type.body, color = Palette.Muted)
            Text("Regularity needs five nights in a row. It compares when you are asleep with the same time the day before.", style = Type.tiny, color = Palette.Faint, modifier = Modifier.padding(top = Space.xs))
        }
        return
    }
    val v = r.value
    val colour = bandColour(regularityWordBand(v.sri))
    val open = metricOpener(MetricRef.Fitness(FitnessCard.REGULARITY))
    SectionCard("REGULARITY", trailing = "last 14 nights · tap for the formula", onClick = open, description = metricDescription(MetricRef.Fitness(FitnessCard.REGULARITY))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(120.dp)) {
                CountUpText("regularity-${data.date}", v.sri.toLong(), Type.hero, color = Palette.Text)
                Text(regularityWord(v.sri), style = Type.body.copy(fontWeight = FontWeight.Medium), color = colour)
                val spread = listOfNotNull(v.bedtimeSpreadMin?.let { "bed ± $it min" }, v.wakeSpreadMin?.let { "wake ± $it min" }).joinToString(" · ")
                if (spread.isNotEmpty()) Text(spread, style = Type.tiny, color = Palette.Muted, modifier = Modifier.padding(top = Space.xs))
            }
            Spacer(Modifier.width(Space.m))
            NightBars(v.nights, zone, colour, Modifier.weight(1f))
        }
    }
}

// One row per night on a shared 20:00–12:00 axis; rows draw in top to bottom.
@Composable
private fun NightBars(nights: List<NightSpan>, zone: ZoneId, colour: Color, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    Column(modifier) {
        nights.forEachIndexed { i, n ->
            val progress = remember(i) { Animatable(if (reduce) 1f else 0f) }
            LaunchedEffect(i) { if (!reduce) { delay(40L * i); progress.animateTo(1f, tween(Motion.RISE, easing = Motion.ease)) } }
            BoxWithConstraints(Modifier.fillMaxWidth().height(5.dp).padding(vertical = 0.5.dp)) {
                val w = maxWidth
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Chart.track))
                if (n.start != null && n.end != null) {
                    val s = axisFraction(n.start.atZone(zone).let { it.hour * 60 + it.minute })
                    val e = axisFraction(n.end.atZone(zone).let { it.hour * 60 + it.minute })
                    if (e > s) Box(Modifier.offset(x = w * s).width(w * (e - s) * progress.value).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colour))
                }
            }
        }
    }
}

private fun axisFraction(minuteOfDay: Int): Float {
    val sinceStart = ((minuteOfDay - AXIS_START_MIN) % (24 * 60) + 24 * 60) % (24 * 60)
    return (sinceStart.toFloat() / AXIS_SPAN_MIN).coerceIn(0f, 1f)
}
