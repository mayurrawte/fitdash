package app.fitdash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import app.fitdash.ui.theme.Palette
import dev.chrisbanes.haze.HazeState

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun GlassNav(items: List<NavItem>, pager: PagerState, hazeState: HazeState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    DockNav(items, pager, onSelect, modifier)
}

private val DockItem = 56.dp
private val DockGap = 6.dp
private val DockShape = RoundedCornerShape(36.dp)

// A solid plum capsule of icon-only circles; the light circle slides with the pager.
@Composable
private fun DockNav(items: List<NavItem>, pager: PagerState, onSelect: (Int) -> Unit, modifier: Modifier) {
    val ink = Palette.Accent
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
