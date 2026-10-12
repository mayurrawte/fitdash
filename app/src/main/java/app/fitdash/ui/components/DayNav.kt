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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.fitdash.ui.LocalPrefs
import app.fitdash.ui.theme.Motion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// The seven days ending on the selected day, so no cell is ever a future day. The selection highlight and number colour animate.
@Composable
fun WeekStrip(days: List<LocalDate>, selected: LocalDate, hasData: Set<LocalDate>, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier, enabled: (LocalDate) -> Boolean = { true }) {
    val haptic = LocalHapticFeedback.current
    val haptics = LocalPrefs.current.haptics
    Row(modifier.fillMaxWidth().padding(horizontal = Space.l)) {
        days.forEach { day ->
            val on = day == selected
            val active = enabled(day)
            val bg by animateColorAsState(if (on) Palette.ChipStrong else Color.Transparent, tween(Motion.PAGE), label = "strip-bg")
            val fg by animateColorAsState(if (on) Palette.Accent else Palette.Text, tween(Motion.PAGE), label = "strip-fg")
            val interaction = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (active) 1f else 0.3f)
                    .pressScale(interaction, 0.92f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable(interactionSource = interaction, indication = null, enabled = active, role = Role.Button) {
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(day)
                    }
                    .heightIn(min = 48.dp)
                    .padding(vertical = Space.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(), style = Type.tiny, color = Palette.Muted)
                Text(
                    day.dayOfMonth.toString(),
                    Modifier.padding(vertical = 3.dp),
                    style = Type.small.copy(fontSize = 15.sp),
                    color = fg,
                )
                Box(Modifier.size(4.dp).clip(CircleShape).background(if (day in hasData) Palette.Accent else Color.Transparent))
            }
        }
    }
}

// 48 dp tap target around a 38 dp visible circle.
@Composable
private fun CircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Palette.Chip), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(17.dp), tint = Palette.Text)
        }
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = Palette.Muted)
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
            ArrowButton(NavIcons.chevronLeft, "Previous day", canPrev, onPrev)
            Text(
                dateText.uppercase(),
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onDate)
                    .semantics { contentDescription = "Pick a date" }
                    .padding(horizontal = Space.s, vertical = Space.m),
                style = Type.label, color = Palette.Muted,
            )
            ArrowButton(NavIcons.chevronRight, "Next day", canNext, onNext)
        }
        CircleButton(NavIcons.info, "Info", onInfo)
    }
}
