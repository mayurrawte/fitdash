package app.fitdash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type

val CardShape = RoundedCornerShape(22.dp)

@Composable
fun SectionCard(
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingColor: Color = Palette.Muted,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .padding(horizontal = Space.l)
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Card)
            .padding(horizontal = Space.l, vertical = Space.l),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = Type.label, color = Palette.Muted)
            if (trailing != null) Text(trailing, style = Type.small, color = trailingColor)
        }
        Spacer(Modifier.height(Space.s))
        content()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = Type.label, color = Palette.Muted, modifier = Modifier.padding(start = Space.xl, end = Space.xl, top = Space.s))
}

@Composable
fun ValueText(value: String, unit: String?, modifier: Modifier = Modifier, style: TextStyle = Type.value, color: Color = Palette.Text) {
    Text(
        buildAnnotatedString {
            append(value)
            if (!unit.isNullOrEmpty()) withStyle(SpanStyle(fontSize = 12.sp, color = Palette.Muted)) { append(" $unit") }
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
