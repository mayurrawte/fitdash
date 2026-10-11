package app.fitdash.ui.pages

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.fitdash.settings.SettingsStore
import app.fitdash.ui.PreviewData
import app.fitdash.ui.components.GlassNav
import app.fitdash.ui.components.LocalRevealed
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.NavItem
import app.fitdash.ui.theme.FitdashTheme
import app.fitdash.ui.theme.LocalTheme
import app.fitdash.ui.theme.Motion
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.ThemeId
import app.fitdash.ui.share.ShareRender
import app.fitdash.ui.theme.ThemedBackground
import app.fitdash.ui.today.TodayUiState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.LocalDate

private val NAV = listOf(
    NavItem("Today", NavIcons.today),
    NavItem("Sleep", NavIcons.sleep),
    NavItem("Activity", NavIcons.activity),
    NavItem("Trends", NavIcons.trends),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagerScreen(state: TodayUiState.Ready, store: SettingsStore, onRefresh: () -> Unit, onSelectDate: (LocalDate) -> Unit) {
    val data = state.data
    val pager = rememberPagerState { NAV.size }
    val scope = rememberCoroutineScope()
    val haze = rememberHazeState()
    val revealed = remember { mutableSetOf<String>() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var metric by remember { mutableStateOf<MetricRef?>(null) }
    val settings by store.flow.collectAsState()
    val today = if (data.isToday) data.date else LocalDate.now()

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    val goTo: (Int) -> Unit = { page -> scope.launch { pager.animateScrollToPage(page, animationSpec = tween(Motion.PAGE, easing = Motion.ease)) } }
    val topColour = LocalTheme.current.bgTop
    val askAi = {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, state.aiText)
        context.startActivity(Intent.createChooser(send, "Ask AI about my data"))
    }
    val chrome = PageChrome(
        date = data.date,
        today = today,
        error = state.error,
        onPrev = { onSelectDate(data.date.minusDays(1)) },
        onNext = { onSelectDate(data.date.plusDays(1)) },
        onPickDate = { showCalendar = true },
        onMenu = { showSettings = true },
        onInfo = { showInfo = true },
        onRetry = onRefresh,
    )

    CompositionLocalProvider(LocalRevealed provides revealed, LocalOpenMetric provides { metric = it }) {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().background(Palette.Background),
        ) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                ThemedBackground()
                HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    when (page) {
                        0 -> TodayPage(data, chrome, { (pager.currentPage - page) + pager.currentPageOffsetFraction }, goTo, onSelectDate, { sharing = true }, askAi)
                        1 -> SleepPage(data, chrome)
                        2 -> ActivityPage(data, chrome, settings.miles)
                        else -> TrendsPage(data, chrome)
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(topColour, topColour.copy(alpha = 0.85f), Color.Transparent))),
            )
            GlassNav(
                NAV,
                pager,
                haze,
                onSelect = { i ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    goTo(i)
                },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp),
            )
        }
        if (sharing) ShareRender(data, onDone = { sharing = false })
        if (showSettings) SettingsSheet(store, data.sources, onDismiss = { showSettings = false })
        if (showInfo) InfoSheet(data.sources, onDismiss = { showInfo = false })
        metric?.let { MetricDetailSheet(it, data, settings.miles, onDismiss = { metric = null }) }
        if (showCalendar) DatePickerSheet(data.date, today, onSelectDate, onDismiss = { showCalendar = false })
    }
}

@Preview(heightDp = 900)
@Composable
private fun TodayTeal() = PagePreview(ThemeId.TEAL, 0)

@Preview(heightDp = 900)
@Composable
private fun TodayPastel() = PagePreview(ThemeId.PASTEL, 0)

@Preview(heightDp = 900)
@Composable
private fun SleepTeal() = PagePreview(ThemeId.TEAL, 1)

@Preview(heightDp = 900)
@Composable
private fun SleepPastel() = PagePreview(ThemeId.PASTEL, 1)

@Composable
private fun PagePreview(theme: ThemeId, page: Int) {
    val data = PreviewData.today(21, 30)
    val chrome = PageChrome(data.date, data.date, false, {}, {}, {}, {}, {}, {})
    FitdashTheme(theme) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground()
            if (page == 0) TodayPage(data, chrome, { 0f }, {}, {}, {}, {}) else SleepPage(data, chrome)
        }
    }
}
