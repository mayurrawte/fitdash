package app.fitdash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type

@Composable
fun StatTile(label: String, value: String, unit: String?, note: String, modifier: Modifier = Modifier, visual: @Composable () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Card)
            .padding(Space.m)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(label.uppercase(), style = Type.label, color = Palette.Muted)
        ValueText(value, unit, Modifier.padding(top = Space.xs, bottom = Space.s), style = Type.tile)
        Spacer(Modifier.weight(1f))
        visual()
        Text(note, style = Type.tiny, color = Palette.Muted, modifier = Modifier.padding(top = Space.s))
    }
}

@Composable
fun TileGrid(tiles: List<@Composable (Modifier) -> Unit>) {
    Column(Modifier.padding(horizontal = Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                row.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
