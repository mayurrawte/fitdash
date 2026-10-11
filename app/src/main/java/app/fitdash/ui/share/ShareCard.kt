package app.fitdash.ui.share

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.fitdash.metrics.ReadinessResult
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.asleepDuration
import app.fitdash.ui.components.ListRow
import app.fitdash.ui.components.NavIcons
import app.fitdash.ui.components.Orb
import app.fitdash.ui.formatDate
import app.fitdash.ui.formatDuration
import app.fitdash.ui.formatSteps
import app.fitdash.ui.formatTime12
import app.fitdash.ui.pages.TrioStat
import app.fitdash.ui.theme.Palette
import app.fitdash.ui.theme.Space
import app.fitdash.ui.theme.ThemedBackground
import app.fitdash.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZoneId

private const val WIDTH = 1080
private const val HEIGHT = 1350
private const val SCALE = 3f

@Composable
fun ShareRender(data: TodayData, onDone: () -> Unit) {
    val context = LocalContext.current
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        try {
            withFrameNanos { }
            withFrameNanos { }
            val bitmap = layer.toImageBitmap().asAndroidBitmap()
            val file = withContext(Dispatchers.IO) {
                File(context.cacheDir, "share").apply { mkdirs() }.resolve("fitdash-${data.date}.png").also { f ->
                    f.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, "Share your day"))
        } finally {
            onDone()
        }
    }

    Box(
        Modifier
            .requiredSize(with(density) { WIDTH.toDp() }, with(density) { HEIGHT.toDp() })
            .drawWithContent {
                layer.record(IntSize(WIDTH, HEIGHT)) { this@drawWithContent.drawContent() }
            },
    ) {
        CompositionLocalProvider(LocalDensity provides Density(SCALE, 1f)) {
            ShareCard(data)
        }
    }
}

@Composable
fun ShareCard(data: TodayData) {
    val zone = ZoneId.systemDefault()
    val score = (data.readiness as? ReadinessResult.Ready)?.score
    Box(Modifier.requiredSize((WIDTH / SCALE).dp, (HEIGHT / SCALE).dp)) {
        ThemedBackground()
        Column(
            Modifier.fillMaxSize().padding(horizontal = Space.xl, vertical = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatDate(data.date).uppercase(), style = Type.label, color = Palette.Muted)
            Spacer(Modifier.height(Space.l))
            Orb(score, diameter = 200.dp, frozen = true) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(score?.toString() ?: "—", style = Type.hero)
                    Text("RECOVERY", style = Type.label)
                }
            }
            Spacer(Modifier.height(Space.l))
            HorizontalDivider(color = Palette.Divider)
            Row(Modifier.fillMaxWidth().padding(vertical = Space.m)) {
                TrioStat("Sleep", data.sleep?.score, data.previous.sleep, Modifier.weight(1f))
                TrioStat("Recovery", score, data.previous.readiness, Modifier.weight(1f))
                TrioStat("Movement", data.movementScore, data.previous.movement, Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                data.sleep?.let {
                    ListRow(NavIcons.bed, "Sleep", "${formatDuration(it.asleep)} · ${formatTime12(it.start, zone)} – ${formatTime12(it.end, zone)}")
                }
                data.naps.forEach {
                    ListRow(NavIcons.bed, "Nap", "${formatDuration(asleepDuration(it))} · ${formatTime12(it.start, zone)} – ${formatTime12(it.end, zone)}")
                }
                data.activity?.let {
                    ListRow(NavIcons.walk, "Steps", "${formatSteps(it.steps)} of ${formatSteps(data.goals.steps.toLong())} · ${it.goalPercent}%")
                }
            }
            Spacer(Modifier.weight(1f))
            Text("FITDASH", style = Type.label, color = Palette.Faint, textAlign = TextAlign.Center)
        }
    }
}
