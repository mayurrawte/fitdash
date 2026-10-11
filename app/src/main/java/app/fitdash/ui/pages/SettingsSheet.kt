package app.fitdash.ui.pages

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import app.fitdash.BuildConfig
import app.fitdash.health.SourceStatus
import app.fitdash.settings.SettingsStore
import app.fitdash.ui.components.ListRow
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.SectionCard
import app.fitdash.ui.components.cardSurface
import app.fitdash.ui.components.SegmentedControl
import app.fitdash.ui.formatSteps
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.THEMES
import app.fitdash.ui.theme.ThemeId
import app.fitdash.ui.theme.ThemeTokens
import app.fitdash.ui.theme.Type
import java.util.Locale

private const val GITHUB_URL = "https://github.com/mayurrawte/fitdash"
private const val WHY =
    "Google Fit and Health Connect hold your data but show it plainly. fitdash is a calm, private dashboard on top of them: no account, no cloud, no internet access."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(store: SettingsStore, sources: List<SourceStatus>, onDismiss: () -> Unit) {
    val settings by store.flow.collectAsState()
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.Background,
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = Space.xxl), verticalArrangement = Arrangement.spacedBy(Space.l)) {
            item { Label("THEME") }
            item { ThemeRow(settings.theme) { id -> store.update { it.copy(theme = id) } } }
            item {
                SectionCard("Goals") {
                    Stepper(
                        "Daily steps", formatSteps(settings.stepGoal.toLong()), settings.stepGoal > 2000, settings.stepGoal < 30000,
                        { store.update { it.copy(stepGoal = it.stepGoal - 500) } }, { store.update { it.copy(stepGoal = it.stepGoal + 500) } },
                    )
                    Stepper(
                        "Sleep", String.format(Locale.ENGLISH, "%.1f h", settings.sleepHours), settings.sleepHours > 5.0, settings.sleepHours < 10.0,
                        { store.update { it.copy(sleepHours = it.sleepHours - 0.5) } }, { store.update { it.copy(sleepHours = it.sleepHours + 0.5) } },
                    )
                }
            }
            item { Label("UNITS") }
            item { SegmentedControl(listOf("km", "miles"), if (settings.miles) 1 else 0, { i -> store.update { it.copy(miles = i == 1) } }) }
            item { Label("DATA") }
            item { ListRow(NavIcons.info, "Data sources", null) { showInfo = true } }
            item {
                ListRow(NavIcons.heart, "Open Health Connect", null) {
                    context.startActivity(HealthConnectClient.getHealthConnectManageDataIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            if (BuildConfig.SHOW_SUPPORT) {
                item { Label("SUPPORT") }
                item { ListRow(NavIcons.cup, "Buy me a coffee", null) { uri.openUri(SUPPORT_URL) } }
            }
            item { Label("ABOUT") }
            item { Text(WHY, Modifier.padding(horizontal = Space.xl), style = Type.body, color = Palette.Muted) }
            item { ListRow(NavIcons.sparkle, "Open source · GPL-3.0", null) { uri.openUri(GITHUB_URL) } }
            item { ListRow(NavIcons.today, "Made by Mayur Rawte", null) }
            item { ListRow(NavIcons.clock, "Version ${BuildConfig.VERSION_NAME}", null) }
        }
    }
    if (showInfo) InfoSheet(sources, onDismiss = { showInfo = false })
}

@Composable
private fun Label(text: String) {
    Text(text, Modifier.padding(start = Space.xl, end = Space.xl, top = Space.s), style = Type.label, color = Palette.Muted)
}

@Composable
private fun ThemeRow(selected: ThemeId, onSelect: (ThemeId) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = THEMES.getValue(selected)
    Box(Modifier.padding(horizontal = Space.l).fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .cardSurface()
                .clickable(role = Role.Button) { open = true }
                .padding(horizontal = Space.l, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniOrb(current, 22.dp)
            Text("Theme", Modifier.weight(1f).padding(horizontal = Space.m), style = Type.body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium), color = Palette.Text)
            Text(selected.label, style = Type.body.copy(fontSize = 13.5.sp), color = Palette.Muted)
            Text("▾", Modifier.padding(start = Space.s), style = Type.body, color = Palette.Faint)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = current.bgMid) {
            THEMES.values.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.id.label, style = Type.body, color = Palette.Text) },
                    leadingIcon = { MiniOrb(t, 16.dp) },
                    trailingIcon = { if (t.id == selected) Text("✓", style = Type.body, color = Palette.Accent) },
                    onClick = { open = false; onSelect(t.id) },
                )
            }
        }
    }
}

@Composable
private fun MiniOrb(t: ThemeTokens, size: Dp) {
    Canvas(Modifier.size(size).clip(CircleShape)) {
        val m = this.size.minDimension
        fun blob(c: Color, at: Offset, r: Float) = Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center = at, radius = r)
        drawRect(lerp(t.orb2, t.orb3, 0.5f))
        drawRect(blob(t.orb3, Offset(m * 0.45f, m * 0.75f), m * 0.45f))
        drawRect(blob(t.orb2, Offset(m * 0.7f, m * 0.4f), m * 0.3f))
        drawRect(blob(t.orb1, Offset(m * 0.3f, m * 0.3f), m * 0.35f))
    }
}

@Composable
private fun Stepper(label: String, value: String, canDec: Boolean, canInc: Boolean, onDec: () -> Unit, onInc: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), Modifier.weight(1f), style = Type.label, color = Palette.Muted)
        StepButton("−", canDec, onDec)
        Text(value, Modifier.padding(horizontal = Space.m), style = Type.body.copy(fontWeight = FontWeight.Medium), color = Palette.Text)
        StepButton("+", canInc, onInc)
    }
}

@Composable
private fun StepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(Palette.Chip)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Type.value, color = Palette.Text) }
}
