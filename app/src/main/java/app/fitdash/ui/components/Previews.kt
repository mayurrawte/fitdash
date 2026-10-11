package app.fitdash.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.fitdash.metrics.Status
import app.fitdash.ui.theme.FitdashTheme
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.ThemeId
import app.fitdash.ui.theme.ThemedBackground
import androidx.compose.foundation.layout.Box
import java.time.LocalDate

private val SunDay = LocalDate.of(2026, 10, 11)

@Composable
private fun ThemePreview(id: ThemeId, content: @Composable () -> Unit) {
    FitdashTheme(id) {
        CompositionLocalProvider(LocalRevealed provides mutableSetOf("gauge-Sleep-83", "span-12:40 AM-9:56 AM")) {
            Box {
                ThemedBackground()
                Column(Modifier.padding(vertical = Space.l)) { content() }
            }
        }
    }
}

@Composable
private fun OrbSample() = Orb(79) {
    Text("79", style = app.fitdash.ui.theme.Type.hero)
}

@Composable
private fun GaugeSample() {
    SemiGauge(83, "Sleep", Status.GOOD)
    SpanBar("12:40 AM", "9:56 AM", from = 0.08f, to = 0.94f, modifier = Modifier.padding(horizontal = 22.dp))
}

@Composable
private fun WeekSample() = WeekStrip((5..11).map { SunDay.withDayOfMonth(it) }, SunDay, setOf(SunDay, SunDay.minusDays(2), SunDay.minusDays(4)), {})

@Composable
private fun TopBarSample() = TopBar("Sun · 11 Oct", canNext = false, onPrev = {}, onNext = {}, onDate = {}, onMenu = {}, onInfo = {})

@Preview(showBackground = true) @Composable private fun OrbTeal() = ThemePreview(ThemeId.TEAL) { OrbSample() }
@Preview(showBackground = true) @Composable private fun OrbPastel() = ThemePreview(ThemeId.PASTEL) { OrbSample() }
@Preview(showBackground = true) @Composable private fun GaugeTeal() = ThemePreview(ThemeId.TEAL) { GaugeSample() }
@Preview(showBackground = true) @Composable private fun GaugePastel() = ThemePreview(ThemeId.PASTEL) { GaugeSample() }
@Preview(showBackground = true) @Composable private fun WeekTeal() = ThemePreview(ThemeId.TEAL) { WeekSample() }
@Preview(showBackground = true) @Composable private fun WeekPastel() = ThemePreview(ThemeId.PASTEL) { WeekSample() }
@Preview(showBackground = true) @Composable private fun TopBarTeal() = ThemePreview(ThemeId.TEAL) { TopBarSample() }
@Preview(showBackground = true) @Composable private fun TopBarPastel() = ThemePreview(ThemeId.PASTEL) { TopBarSample() }
