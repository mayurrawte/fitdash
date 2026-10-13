package app.fitdash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.ui.theme.LocalTheme
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Type
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

data class NavItem(val label: String, val icon: ImageVector)

private val ItemWidth = 74.dp
private val ItemHeight = 48.dp
private val BarShape = RoundedCornerShape(26.dp)
private val PillShape = RoundedCornerShape(20.dp)

@Composable
fun GlassNav(items: List<NavItem>, pager: PagerState, hazeState: HazeState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (LocalTheme.current.soft) return DockNav(items, pager, onSelect, modifier)
    val background = Palette.Background
    val glass = Palette.Glass
    val hairline = Palette.Divider
    val activeFill = Palette.ChipStrong
    Box(
        modifier
            .shadow(18.dp, BarShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(BarShape)
            .hazeBlur(
                HazeInput.Sources(hazeState),
                HazeBlurStyle {
                    blurRadius(20.dp)
                    backgroundColor(background)
                    colorEffects(listOf(HazeColorEffect.tint(glass)))
                    noiseFactor(0f)
                },
            )
            .border(1.dp, hairline, BarShape)
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .size(ItemWidth, ItemHeight)
                .graphicsLayer { translationX = (pager.currentPage + pager.currentPageOffsetFraction) * ItemWidth.toPx() }
                .clip(PillShape)
                .background(activeFill),
        )
        Row {
            items.forEachIndexed { i, item ->
                val selected = pager.currentPage == i
                Column(
                    Modifier
                        .size(ItemWidth, ItemHeight)
                        .clip(PillShape)
                        .clickable { onSelect(i) }
                        .clearAndSetSemantics { contentDescription = item.label; this.selected = selected; role = Role.Tab },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val tint = if (selected) Palette.Text else Palette.Muted
                    Icon(item.icon, null, Modifier.size(18.dp), tint = tint)
                    Text(item.label.uppercase(), style = Type.tiny.copy(fontSize = 10.sp), color = tint, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

private val DockItem = 56.dp
private val DockGap = 6.dp
private val DockShape = RoundedCornerShape(36.dp)

// Soft's nav: a solid ink capsule of icon-only circles; the light circle slides with the pager.
@Composable
private fun DockNav(items: List<NavItem>, pager: PagerState, onSelect: (Int) -> Unit, modifier: Modifier) {
    val ink = Palette.Glass
    val ground = Palette.Background
    Box(
        modifier
            .shadow(16.dp, DockShape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(DockShape)
            .background(ink)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .size(DockItem)
                .graphicsLayer { translationX = (pager.currentPage + pager.currentPageOffsetFraction) * (DockItem + DockGap).toPx() }
                .clip(CircleShape)
                .background(ground),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DockGap)) {
            items.forEachIndexed { i, item ->
                val selected = pager.currentPage == i
                Box(
                    Modifier
                        .size(DockItem)
                        .clip(CircleShape)
                        .clickable { onSelect(i) }
                        .clearAndSetSemantics { contentDescription = item.label; this.selected = selected; role = Role.Tab },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, null, Modifier.size(22.dp), tint = if (selected) ink else ground.copy(alpha = 0.75f))
                }
            }
        }
    }
}
