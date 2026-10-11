package app.fitdash.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.fitdash.R

object Palette {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.bgBottom
    val Card: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.card
    val Divider: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.hairline
    val Text: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.text
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.muted
    val Faint: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.faint
    val Calibrating: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.faint
    val Green: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.good
    val DimGreen: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.good.copy(alpha = 0.45f)
    val Hrv: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.accent
    val Awake: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.awake
    val Rem: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.rem
    val Light: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.light
    val Deep: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.deep
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.warn
    val Glass: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.navBg
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.accent
    val Good: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.good
    val Warn: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.warn
    val Bad: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.bad
    val Chip: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.chip
    val ChipStrong: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.chipStrong
    val OnOrb: Color @Composable @ReadOnlyComposable get() = LocalTheme.current.onOrb
}

val Inter = FontFamily(
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

object Type {
    private val bodyFamily: FontFamily @Composable @ReadOnlyComposable get() = if (LocalTheme.current.monoEverywhere) Mono else Inter

    val hero: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 72.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-0.03).em, fontFeatureSettings = "tnum")
    val title: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val value: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val tile: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 24.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val body: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 14.sp, fontWeight = FontWeight.Light, lineHeight = 21.sp)
    val small: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val label: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.08.em)
    val tiny: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = Mono, fontSize = 9.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.08.em)
    val unit: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = Mono, fontSize = 9.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.08.em)
}

object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Composable
fun FitdashTheme(themeId: ThemeId = ThemeId.TEAL, content: @Composable () -> Unit) {
    val t = THEMES.getValue(themeId)
    val scheme = if (t.lightSystemBars) {
        lightColorScheme(primary = t.accent, background = t.bgBottom, surface = t.bgMid, onBackground = t.text, onSurface = t.text)
    } else {
        darkColorScheme(primary = t.accent, background = t.bgBottom, surface = t.bgMid, onBackground = t.text, onSurface = t.text)
    }
    CompositionLocalProvider(LocalTheme provides t) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun ThemedBackground(modifier: Modifier = Modifier) {
    val t = LocalTheme.current
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(t.bgTop, t.bgMid, t.bgBottom)))
        if (t.grid) {
            val step = 22.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(t.hairline, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(t.hairline, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }
    }
}
