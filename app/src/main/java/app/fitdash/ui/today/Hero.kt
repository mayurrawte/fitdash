package app.fitdash.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import app.fitdash.metrics.ActivitySummary
import app.fitdash.metrics.HeroMode
import app.fitdash.metrics.ReadinessResult
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
import app.fitdash.ui.bandAdvice
import app.fitdash.ui.bandWord
import app.fitdash.ui.basedOn
import app.fitdash.ui.components.CountUpText
import app.fitdash.ui.components.Orb
import app.fitdash.ui.eveningAdvice
import app.fitdash.ui.eveningWord
import app.fitdash.ui.formatDate
import app.fitdash.ui.formatSteps
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor

@Composable
fun Hero(data: TodayData, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDate(data.date), style = Type.small, color = Palette.Muted)
            Text("Today", style = Type.small, color = Palette.Muted)
        }
        Spacer(Modifier.height(40.dp))
        when (data.hero) {
            HeroMode.READINESS -> ReadinessHero(data.readiness)
            HeroMode.ACTIVITY -> ActivityHero(data.activity)
        }
        Spacer(Modifier.height(16.dp))
        Text("scroll ⌄", style = Type.small, color = Palette.Faint)
    }
}

@Composable
private fun ReadinessHero(r: ReadinessResult) {
    when (r) {
        is ReadinessResult.Ready ->
            HeroBody(r.score, "READINESS", r.score.toLong(), bandWord(r.band), bandAdvice(r.band), basedOn(r.used))
        is ReadinessResult.Calibrating ->
            HeroBody(null, "CALIBRATING", null, "Calibrating · night ${r.nights} of ${Tuning.MIN_NIGHTS}", "Keep wearing your Fitbit to bed.", null)
        ReadinessResult.NoData ->
            HeroBody(null, "READINESS", null, "No data for last night", "Sync your Fitbit, then pull down to refresh.", null)
    }
}

@Composable
private fun ActivityHero(a: ActivitySummary?) {
    if (a == null) {
        HeroBody(null, "STEPS", null, "No steps yet today", "Sync your Fitbit, then pull down to refresh.", null)
    } else {
        HeroBody(a.goalPercent, "STEPS", a.steps, eveningWord(a.goalPercent), eveningAdvice(a), null, sub = "of ${formatSteps(Tuning.STEP_GOAL.toLong())}")
    }
}

@Composable
private fun HeroBody(colourScore: Int?, label: String, number: Long?, word: String, advice: String, basedOn: String?, sub: String? = null) {
    val numberStyle = if ((number ?: 0L) > 999) Type.hero.copy(fontSize = 54.sp) else Type.hero
    Orb(colourScore) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = Type.label)
            if (number != null) CountUpText(number, numberStyle) else Text("—", style = numberStyle)
            sub?.let { Text(it, style = Type.label) }
        }
    }
    Spacer(Modifier.height(20.dp))
    Text(word, style = Type.body, color = colourScore?.let(::scoreColor) ?: Palette.Muted)
    Text(
        advice,
        style = Type.body,
        color = Palette.Text.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 30.dp, vertical = 6.dp),
    )
    basedOn?.let { Text(it, style = Type.small, color = Palette.Muted) }
}
