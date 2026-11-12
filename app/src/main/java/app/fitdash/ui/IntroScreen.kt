package app.fitdash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Type
import kotlinx.coroutines.launch

private val SLIDES = listOf(
    Triple(NavIcons.today, "Recovery at a glance", "One score from your sleep, HRV and resting heart rate, set against your own usual."),
    Triple(NavIcons.sleep, "Sleep, calmly", "Stages, regularity and sleep debt, with no alarm over a single night."),
    Triple(NavIcons.activity, "Activity and trends", "Steps, workouts and heart rate, with week and month views."),
    Triple(NavIcons.log, "Works without a watch", "Your phone counts steps, and you can log water and sleep by hand."),
    Triple(NavIcons.heart, "Private by design", "No account, no ads and no internet access. Your data stays on this phone."),
)

// Shown once after the name step: what fitdash does, then how much of it to show.
@Composable
fun IntroScreen(onDone: (focus: Boolean) -> Unit) {
    val pager = rememberPagerState { SLIDES.size + 1 }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Palette.Background).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                if (page < SLIDES.size) {
                    val (icon, title, body) = SLIDES[page]
                    Box(Modifier.size(96.dp).clip(CircleShape).background(Palette.ChipStrong), contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(40.dp), tint = Palette.Accent)
                    }
                    Text(title, style = Type.title, color = Palette.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 32.dp))
                    Text(body, style = Type.body, color = Palette.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                } else {
                    Text("How much do you want to see?", style = Type.title, color = Palette.Text, textAlign = TextAlign.Center)
                    Button(
                        onClick = { onDone(true) },
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent, contentColor = Palette.OnAccent),
                    ) { Text("Focus") }
                    Text("The essentials: recovery, sleep, steps and water.", style = Type.small, color = Palette.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    OutlinedButton(onClick = { onDone(false) }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Advanced", color = Palette.Text) }
                    Text("Every card, chart and detail.", style = Type.small, color = Palette.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Text("You can change this in Settings.", style = Type.small, color = Palette.Faint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(SLIDES.size + 1) { i -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (i == pager.currentPage) Palette.Accent else Palette.Divider)) }
        }
        if (pager.currentPage < SLIDES.size) Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { scope.launch { pager.animateScrollToPage(SLIDES.size) } }) { Text("Skip", color = Palette.Muted) }
            TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("Next", color = Palette.Accent) }
        }
    }
}
