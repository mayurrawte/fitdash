package app.fitdash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.fitdash.metrics.Status
import app.fitdash.ui.theme.Mono
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type

val CardShape = RoundedCornerShape(18.dp)

@Composable
fun Modifier.cardSurface(): Modifier = clip(CardShape).background(Palette.Card).border(1.dp, Palette.Divider, CardShape)

fun Modifier.tappable(onClick: (() -> Unit)?, description: String?): Modifier =
    if (onClick == null) this
    else clickable(role = Role.Button, onClick = onClick).semantics { if (description != null) contentDescription = description }

@Composable
fun SectionCard(
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingColor: Color = Palette.Muted,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .padding(horizontal = Space.l)
            .fillMaxWidth()
            .cardSurface()
            .tappable(onClick, description)
            .padding(horizontal = Space.l, vertical = Space.l),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = Type.label, color = Palette.Muted)
            if (trailing != null) Text(trailing.uppercase(), style = Type.tiny, color = trailingColor)
        }
        Spacer(Modifier.height(Space.s))
        content()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), style = Type.label, color = Palette.Muted, modifier = Modifier.padding(start = Space.xl, end = Space.xl, top = Space.s))
}

@Composable
fun ValueText(value: String, unit: String?, modifier: Modifier = Modifier, style: TextStyle = Type.value, color: Color = Palette.Text) {
    Text(
        buildAnnotatedString {
            append(value)
            if (!unit.isNullOrEmpty()) withStyle(SpanStyle(fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.08.em, color = Palette.Muted)) { append(" ${unit.uppercase()}") }
        },
        modifier,
        style = style,
        color = color,
    )
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Space.xxl, vertical = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Orb(null, diameter = 140.dp) {}
        Spacer(Modifier.height(Space.xl))
        Text(title, style = Type.value, color = Palette.Text, textAlign = TextAlign.Center)
        Text(body, style = Type.body, color = Palette.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Space.s))
    }
}

@Composable
fun Reveal(key: String, index: Int = 0, content: @Composable () -> Unit) {
    val progress = rememberDrawProgress("reveal-$key", Motion.RISE, index * 60)
    Box(Modifier.graphicsLayer { alpha = progress.value; translationY = (1 - progress.value) * 20.dp.toPx() }) { content() }
}

@Composable
fun EmptyTrack() {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Chart.track))
}

fun statusWord(status: Status) = when (status) {
    Status.OPTIMAL -> "OPTIMAL"
    Status.GOOD -> "GOOD"
    Status.FAIR -> "FAIR"
    Status.ATTENTION -> "NEEDS ATTENTION"
}

@Composable
fun StatusText(status: Status, modifier: Modifier = Modifier) {
    val colour = when (status) {
        Status.OPTIMAL, Status.GOOD -> Palette.Good
        Status.FAIR -> Palette.Warn
        Status.ATTENTION -> Palette.Bad
    }
    Text(statusWord(status), modifier, style = Type.tiny.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium), color = colour)
}

@Composable
fun NumberWithUnits(parts: List<Pair<String, String>>, modifier: Modifier = Modifier, style: TextStyle = Type.tile) {
    Row(modifier) {
        parts.forEachIndexed { i, (number, unit) ->
            Text(number, Modifier.alignByBaseline(), style = style, color = Palette.Text)
            Text(
                unit.uppercase(),
                Modifier.alignByBaseline().padding(start = 2.dp, end = if (i < parts.lastIndex) Space.xs + 2.dp else 0.dp),
                style = Type.unit,
                color = Palette.Muted,
            )
        }
    }
}

@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(start = Space.xl, end = Space.xl, top = Space.s),
        style = Type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
        color = Palette.Text,
    )
}

@Composable
fun ListRow(icon: ImageVector, title: String, subtitle: String?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier
            .padding(horizontal = Space.l)
            .fillMaxWidth()
            .cardSurface()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Space.l, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Palette.ChipStrong), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(16.dp), tint = Palette.Accent)
        }
        Column(Modifier.weight(1f).padding(horizontal = Space.m)) {
            Text(title, style = Type.body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium), color = Palette.Text)
            if (subtitle != null) Text(subtitle, style = Type.small.copy(fontSize = 11.sp), color = Palette.Muted, modifier = Modifier.padding(top = 2.dp))
        }
        if (onClick != null) Text("›", style = Type.body, color = Palette.Faint)
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(modifier.padding(horizontal = Space.l).fillMaxWidth().clip(shape).background(Palette.Chip).padding(3.dp)) {
        options.forEachIndexed { i, option ->
            val on = i == selected
            Text(
                option.uppercase(),
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Palette.ChipStrong else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(vertical = Space.s),
                style = Type.label,
                color = if (on) Palette.Text else Palette.Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
