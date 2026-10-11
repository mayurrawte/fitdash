package app.fitdash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekStrip(days: List<LocalDate>, selected: LocalDate, hasData: Set<LocalDate>, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier, enabled: (LocalDate) -> Boolean = { true }) {
    Row(modifier.fillMaxWidth().padding(horizontal = Space.l)) {
        days.forEach { day ->
            val on = day == selected
            val active = enabled(day)
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (active) 1f else 0.3f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) Palette.ChipStrong else Color.Transparent)
                    .clickable(enabled = active, role = Role.Button) { onSelect(day) }
                    .padding(vertical = Space.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(), style = Type.tiny, color = Palette.Muted)
                Text(
                    day.dayOfMonth.toString(),
                    Modifier.padding(vertical = 3.dp),
                    style = Type.small.copy(fontSize = 15.sp),
                    color = if (on) Palette.Accent else Palette.Text,
                )
                Box(Modifier.size(4.dp).clip(CircleShape).background(if (day in hasData) Palette.Accent else Color.Transparent))
            }
        }
    }
}

@Composable
private fun CircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Palette.Chip)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = Palette.Text)
    }
}

@Composable
fun TopBar(
    dateText: String,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDate: () -> Unit,
    onMenu: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
    canPrev: Boolean = true,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = Space.l), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        CircleButton(NavIcons.menu, "Menu", onMenu)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹",
                Modifier.alpha(if (canPrev) 1f else 0.3f).clickable(enabled = canPrev, role = Role.Button, onClick = onPrev).padding(horizontal = Space.m, vertical = Space.s),
                style = Type.label, color = Palette.Muted, textAlign = TextAlign.Center,
            )
            Text(dateText.uppercase(), Modifier.clickable(onClick = onDate), style = Type.label, color = Palette.Muted)
            Text(
                "›",
                Modifier.alpha(if (canNext) 1f else 0.3f).clickable(enabled = canNext, role = Role.Button, onClick = onNext).padding(horizontal = Space.m, vertical = Space.s),
                style = Type.label, color = Palette.Muted, textAlign = TextAlign.Center,
            )
        }
        CircleButton(NavIcons.info, "Info", onInfo)
    }
}
