package app.fitdash.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.fitdash.BuildConfig
import app.fitdash.health.SourceStatus
import app.fitdash.ui.sourceLine
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.Type

const val SUPPORT_URL = "https://buymeacoffee.com/thepsygeek"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(sources: List<SourceStatus>, onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.Background) {
        LazyColumn(contentPadding = PaddingValues(start = Space.xl, end = Space.xl, bottom = Space.xxl)) {
            item {
                Text("About your data", style = Type.value, color = Palette.Text)
                Text(
                    "fitdash has no internet access. Your data never leaves this phone unless you share it.",
                    style = Type.body,
                    color = Palette.Muted,
                    modifier = Modifier.padding(top = Space.s, bottom = Space.xl),
                )
                Text("DATA SOURCES", style = Type.label, color = Palette.Muted, modifier = Modifier.padding(bottom = Space.s))
            }
            items(sources, key = { it.label }) { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = Space.s), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s.label, style = Type.small, color = Palette.Text)
                    Text(sourceLine(s), style = Type.small, color = if (s.hasData) Palette.Green else Palette.Faint)
                }
                HorizontalDivider(color = Palette.Divider)
            }
            if (BuildConfig.SHOW_SUPPORT) {
                item {
                    Row(
                        Modifier
                            .padding(top = Space.xl)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { uri.openUri(SUPPORT_URL) }
                            .semantics { role = Role.Button }
                            .padding(Space.l),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("☕  Support fitdash", style = Type.body, color = Palette.Amber)
                    }
                }
            }
            item {
                Text("fitdash ${BuildConfig.VERSION_NAME}", style = Type.tiny, color = Palette.Faint, modifier = Modifier.padding(top = Space.l))
            }
        }
    }
}
