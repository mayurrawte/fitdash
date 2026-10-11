package app.fitdash.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.metrics.Good
import app.fitdash.metrics.ReadinessResult
import app.fitdash.metrics.SuggestionKind
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.Tuning
import app.fitdash.ui.bandAdvice
import app.fitdash.ui.bandWord
import app.fitdash.ui.basedOn
import app.fitdash.ui.components.CountUpText
import app.fitdash.ui.components.EmptyTrack
import app.fitdash.ui.components.MiniRing
import app.fitdash.ui.components.Orb
import app.fitdash.ui.components.RangeBar
import app.fitdash.ui.components.ScoreRing
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.SectionLabel
import app.fitdash.ui.components.StatTile
import app.fitdash.ui.components.TileGrid
import app.fitdash.ui.formatDuration
import app.fitdash.ui.rangeNote
import app.fitdash.ui.suggestionText
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor
import kotlin.math.roundToInt

private fun suggestionIcon(kind: SuggestionKind) = when (kind) {
    SuggestionKind.HARD_WORKOUT -> "🏋"
    SuggestionKind.MODERATE -> "🚶"
    SuggestionKind.REST -> "🛋"
    SuggestionKind.EARLIER_BED -> "🌙"
    SuggestionKind.EASY_ON_STIMULANTS -> "☕"
    SuggestionKind.WALK -> "👟"
}

@Composable
fun TodayPage(data: TodayData, chrome: PageChrome, pageOffset: () -> Float, onOpen: (Int) -> Unit, onAskAi: () -> Unit) {
    val r = data.readiness
    val score = (r as? ReadinessResult.Ready)?.score
    PageColumn(chrome, accent = score?.let(::scoreColor) ?: Palette.Calibrating) {
        item(key = "orb") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = pageOffset() * size.width * 0.3f }
                    .semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Orb(score, Modifier.padding(top = Space.s)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (r is ReadinessResult.Calibrating) "CALIBRATING" else "RECOVERY", style = Type.label)
                        if (score != null) CountUpText("orb-score", score.toLong(), Type.hero) else Text("—", style = Type.hero)
                    }
                }
                val (word, advice) = when (r) {
                    is ReadinessResult.Ready -> bandWord(r.band) to bandAdvice(r.band)
                    is ReadinessResult.Calibrating -> "Calibrating · night ${r.nights} of ${Tuning.MIN_NIGHTS}" to "Keep wearing your Fitbit to bed."
                    ReadinessResult.NoData -> "No data for last night" to "Sync your Fitbit, then pull down to refresh."
                }
                Text(word, style = Type.body, color = score?.let(::scoreColor) ?: Palette.Muted, modifier = Modifier.padding(top = Space.l))
                Text(
                    advice,
                    style = Type.body,
                    color = Palette.Text.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Space.xxl, vertical = Space.xs),
                )
                (r as? ReadinessResult.Ready)?.let { ready -> basedOn(ready.used)?.let { Text(it, style = Type.small, color = Palette.Muted) } }
            }
        }
        item(key = "rings") {
            Row(Modifier.fillMaxWidth().padding(top = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.l, Alignment.CenterHorizontally)) {
                ScoreRing("ring-recovery", "Recovery", score, 0) {}
                ScoreRing("ring-sleep", "Sleep", data.sleep?.score, 1) { onOpen(1) }
                ScoreRing("ring-movement", "Movement", data.movementScore, 2) { onOpen(2) }
            }
        }
        val h = data.heart
        val tiles = buildList<@Composable (Modifier) -> Unit> {
            h?.hrv?.let { v ->
                add { m ->
                    StatTile("HRV", v.roundToInt().toString(), "ms", rangeNote(v, h.hrvRange, Good.HIGHER), m) {
                        h.hrvRange?.let { RangeBar("today-hrv", v, it, Good.HIGHER) } ?: EmptyTrack()
                    }
                }
            }
            h?.restingHr?.let { v ->
                add { m ->
                    StatTile("Resting HR", v.roundToInt().toString(), "bpm", rangeNote(v, h.restingHrRange, Good.LOWER), m) {
                        h.restingHrRange?.let { RangeBar("today-rhr", v, it, Good.LOWER) } ?: EmptyTrack()
                    }
                }
            }
            data.sleep?.let { s ->
                add { m ->
                    StatTile("Sleep", s.score.toString(), null, formatDuration(s.asleep), m) {
                        MiniRing("today-sleep", s.score / 100f, scoreColor(s.score))
                    }
                }
            }
        }
        if (tiles.isNotEmpty()) {
            item(key = "contributors-label") { SectionLabel("CONTRIBUTORS") }
            item(key = "contributors") { TileGrid(tiles) }
        }
        if (data.suggestions.isNotEmpty()) {
            revealItem("suggestions", 0) {
                SectionCard("DO THIS TODAY") {
                    data.suggestions.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider(color = Palette.Divider)
                        Row(Modifier.padding(vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Palette.Divider),
                                contentAlignment = Alignment.Center,
                            ) { Text(suggestionIcon(s.kind), fontSize = 14.sp) }
                            Text(suggestionText(s), style = Type.body, color = Palette.Text, modifier = Modifier.padding(start = Space.m))
                        }
                    }
                }
            }
        }
        item(key = "ask") { AskAiButton(onAskAi) }
    }
}

@Composable
private fun AskAiButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .padding(horizontal = Space.l)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Palette.Rem.copy(alpha = 0.25f), Palette.Hrv.copy(alpha = 0.2f))))
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = "Ask AI about my data"; role = Role.Button }
            .padding(Space.l),
        contentAlignment = Alignment.Center,
    ) {
        Text("✦  Ask AI about my data", style = Type.body, color = Palette.Text)
    }
}
