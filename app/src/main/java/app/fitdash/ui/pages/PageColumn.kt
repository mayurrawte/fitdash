package app.fitdash.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fitdash.ui.components.Reveal
import app.fitdash.ui.formatDate
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type
import java.time.LocalDate

data class PageChrome(val date: LocalDate, val error: Boolean, val onInfo: () -> Unit, val onRetry: () -> Unit)

@Composable
fun PageColumn(chrome: PageChrome, accent: Color, content: LazyListScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width / 2, size.width * 0.35f),
                        radius = size.width * 0.85f,
                    ),
                )
            },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = top + Space.s, bottom = bottom + 110.dp),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            item(key = "top") { TopRow(chrome) }
            if (chrome.error) item(key = "error") { ErrorBanner(chrome.onRetry) }
            content()
        }
    }
}

@Composable
private fun TopRow(chrome: PageChrome) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(formatDate(chrome.date), style = Type.small, color = Palette.Muted)
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .clickable(onClick = chrome.onInfo)
                .semantics { contentDescription = "About your data"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Text("i", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = Palette.Text)
        }
    }
}

@Composable
private fun ErrorBanner(onRetry: () -> Unit) {
    Text(
        "Couldn't refresh. Tap to retry.",
        style = Type.small,
        color = Palette.Amber,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onRetry).padding(Space.m),
    )
}

fun LazyListScope.revealItem(key: String, index: Int, content: @Composable () -> Unit) {
    item(key = key) { Reveal(key, index) { content() } }
}

fun LazyListScope.pageHeader(kicker: String, title: String?, sub: String?, subColor: Color? = null) {
    item(key = "header") {
        Column(Modifier.padding(horizontal = Space.xl, vertical = Space.s)) {
            Text(kicker, style = Type.label, color = Palette.Muted)
            if (title != null) Text(title, style = Type.title, color = Palette.Text, modifier = Modifier.padding(top = Space.xs))
            if (sub != null) Text(sub, style = Type.small, color = subColor ?: Palette.Green, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
