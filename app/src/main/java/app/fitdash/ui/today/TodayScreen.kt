package app.fitdash.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.fitdash.metrics.TodayData
import app.fitdash.ui.PreviewData
import app.fitdash.ui.miniLine
import app.fitdash.ui.miniScore
import app.fitdash.ui.theme.FitdashTheme
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Type
import app.fitdash.ui.theme.scoreColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(state: TodayUiState.Ready, onRefresh: () -> Unit) {
    val data = state.data
    val list = rememberLazyListState()
    val collapseDistance = with(LocalDensity.current) { 240.dp.toPx() }
    val collapse by remember {
        derivedStateOf {
            if (list.firstVisibleItemIndex > 0) 1f
            else (list.firstVisibleItemScrollOffset / collapseDistance).coerceIn(0f, 1f)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().background(Palette.Background),
    ) {
        LazyColumn(
            state = list,
            contentPadding = WindowInsets.systemBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                Hero(
                    data,
                    Modifier.graphicsLayer {
                        val t = collapse
                        translationY = if (list.firstVisibleItemIndex == 0) list.firstVisibleItemScrollOffset * 0.35f else 0f
                        scaleX = 1 - 0.45f * t
                        scaleY = 1 - 0.45f * t
                        alpha = 1 - t
                    },
                )
            }
            if (state.error) item(key = "error") { ErrorBanner(onRefresh) }
            items(data.cards, key = { it.name }) { kind -> RevealOnEnter { TodayCard(kind, data) } }
            item(key = "end") {
                Text(
                    "that's everything for today",
                    style = Type.small,
                    color = Palette.Faint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 30.dp),
                )
            }
        }
        MiniHeader(data, visible = collapse > 0.75f)
    }
}

@Composable
private fun RevealOnEnter(content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    Box(Modifier.graphicsLayer { alpha = a.value; translationY = (1 - a.value) * 20.dp.toPx() }) { content() }
}

@Composable
private fun MiniHeader(data: TodayData, visible: Boolean) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Palette.Background, Palette.Background, Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(miniScore(data)?.let(::scoreColor) ?: Palette.Calibrating))
            Spacer(Modifier.width(10.dp))
            Text(miniLine(data), style = Type.body, color = Palette.Text)
        }
    }
}

@Composable
private fun ErrorBanner(onRetry: () -> Unit) {
    Text(
        "Couldn't refresh. Tap to retry.",
        style = Type.small,
        color = Palette.Awake,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onRetry).padding(12.dp),
    )
}

@Preview(heightDp = 1800)
@Composable
private fun MorningPreview() {
    FitdashTheme { TodayScreen(TodayUiState.Ready(PreviewData.today(8, 10)), onRefresh = {}) }
}

@Preview(heightDp = 1800)
@Composable
private fun EveningPreview() {
    FitdashTheme { TodayScreen(TodayUiState.Ready(PreviewData.today(21, 30)), onRefresh = {}) }
}
