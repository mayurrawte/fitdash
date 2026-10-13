package app.fitdash.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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

// Optional typefaces for the big numbers only; labels stay mono and body stays Inter.
val SpaceGrotesk = FontFamily(Font(R.font.space_grotesk_light, FontWeight.Light), Font(R.font.space_grotesk_light, FontWeight.ExtraLight))
val InstrumentSerif = FontFamily(Font(R.font.instrument_serif_regular, FontWeight.Normal), Font(R.font.instrument_serif_regular, FontWeight.Light), Font(R.font.instrument_serif_regular, FontWeight.ExtraLight))

// The Soft theme's typeface, for body and labels there and as a numbers option everywhere.
val Outfit = FontFamily(
    Font(R.font.outfit_light, FontWeight.ExtraLight),
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
)

enum class NumbersFont(val label: String, val sample: String) { OUTFIT("Outfit", "Friendly"), INTER("Inter", "Clean"), GROTESK("Space Grotesk", "Geometric"), SERIF("Instrument Serif", "Editorial") }

val LocalNumbersFont = staticCompositionLocalOf { NumbersFont.INTER }

@Composable
@ReadOnlyComposable
fun displayFamily(): FontFamily = when (LocalNumbersFont.current) {
    NumbersFont.OUTFIT -> Outfit
    NumbersFont.INTER -> Inter
    NumbersFont.GROTESK -> SpaceGrotesk
    NumbersFont.SERIF -> InstrumentSerif
}

object Type {
    private val soft: Boolean @Composable @ReadOnlyComposable get() = LocalTheme.current.soft
    private val bodyFamily: FontFamily @Composable @ReadOnlyComposable get() = if (soft) Outfit else Inter
    // Soft drops the mono labels for its own face, a weight heavier so small text holds on the light ground.
    private val labelFamily: FontFamily @Composable @ReadOnlyComposable get() = if (soft) Outfit else Mono
    private val bodyWeight: FontWeight @Composable @ReadOnlyComposable get() = if (soft) FontWeight.Normal else FontWeight.Light
    // The serif has no light weight and sits wider, so numbers in it are set regular and a touch tighter.
    // Outfit is set regular too: its light weight is too thin for big numbers on a light ground.
    private val serif: Boolean @Composable @ReadOnlyComposable get() = LocalNumbersFont.current == NumbersFont.SERIF
    private val regular: Boolean @Composable @ReadOnlyComposable get() = serif || LocalNumbersFont.current == NumbersFont.OUTFIT
    private val displayWeight: FontWeight @Composable @ReadOnlyComposable get() = if (regular) FontWeight.Normal else FontWeight.Light
    private val heroWeight: FontWeight @Composable @ReadOnlyComposable get() = if (regular) FontWeight.Normal else FontWeight.ExtraLight

    val hero: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontSize = 68.sp, fontWeight = heroWeight, letterSpacing = (-0.03).em, fontFeatureSettings = "tnum")
    val title: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontSize = 30.sp, fontWeight = displayWeight, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val value: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontSize = 26.sp, fontWeight = displayWeight, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val tile: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = displayFamily(), fontSize = 24.sp, fontWeight = displayWeight, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val body: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 14.sp, fontWeight = bodyWeight, lineHeight = 21.sp)
    val small: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal)
    // 11 sp is the floor for mono labels: anything smaller is unreadable on a phone at 1.0x.
    val label: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = labelFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.08.em)
    val tiny: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = labelFamily, fontSize = 11.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.08.em)
    val unit: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = labelFamily, fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.08.em)
    val status: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = labelFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.08.em)
    val secondary: TextStyle @Composable @ReadOnlyComposable get() =
        TextStyle(fontFamily = bodyFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal)
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
    val scheme = if (t.soft) lightColorScheme(primary = t.accent, background = t.bgBottom, surface = t.card, onBackground = t.text, onSurface = t.text)
    else darkColorScheme(primary = t.accent, background = t.bgBottom, surface = t.bgMid, onBackground = t.text, onSurface = t.text)
    CompositionLocalProvider(LocalTheme provides t) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun ThemedBackground(modifier: Modifier = Modifier) {
    val t = LocalTheme.current
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(t.bgTop, t.bgMid, t.bgBottom)))
    }
}

// Accessibility: when on, draw-in, count-up and reveal animations finish immediately and the orb holds still.
val LocalReduceMotion = staticCompositionLocalOf { false }

// Inside a tinted tile the muted and faint text take the tile's own ink, so every child reads on the pastel without knowing about it.
@Composable
fun TintScope(tint: Tint?, content: @Composable () -> Unit) {
    val t = LocalTheme.current
    if (tint == null || t.tints.isEmpty()) return content()
    val ink = t.tintInk[tint.ordinal]
    CompositionLocalProvider(LocalTheme provides t.copy(card = t.tints[tint.ordinal], muted = ink, faint = ink, hairline = ink.copy(alpha = 0.15f)), content = content)
}

@Composable
@ReadOnlyComposable
fun tintColor(tint: Tint?): Color? = tint?.let { LocalTheme.current.tints.getOrNull(it.ordinal) }
