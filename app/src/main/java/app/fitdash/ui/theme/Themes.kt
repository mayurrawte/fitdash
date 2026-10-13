package app.fitdash.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeId(val label: String) { SOFT("Soft"), TEAL("Midnight Teal"), GRAPHITE("Graphite") }

data class ThemeTokens(
    val id: ThemeId,
    val bgTop: Color, val bgMid: Color, val bgBottom: Color,
    val text: Color, val muted: Color, val faint: Color, val accent: Color,
    val good: Color, val warn: Color, val bad: Color,
    val card: Color, val hairline: Color, val chip: Color, val chipStrong: Color, val navBg: Color,
    val orb1: Color, val orb2: Color, val orb3: Color, val glow: Color, val onOrb: Color,
    val deep: Color, val light: Color, val rem: Color, val awake: Color,
    // Soft is the light pastel look: white cards, tinted tiles, a dark dock and a ring hero instead of the orb.
    val soft: Boolean = false,
    // A light ground: light Material scheme, dark status-bar icons.
    val lightGround: Boolean = false,
    // Tile tints in order sky, sage, lavender, peach, with a dark ink of each for text on them. Empty: tiles stay plain cards.
    val tints: List<Color> = emptyList(), val tintInk: List<Color> = emptyList(),
)

val THEMES: Map<ThemeId, ThemeTokens> = listOf(
    // Accent is a deep terracotta so it holds 4.5:1 as text on white; the pale peach fill is chipStrong.
    ThemeTokens(ThemeId.SOFT, Color(0xFFF2F3F0), Color(0xFFF2F3F0), Color(0xFFF2F3F0),
        Color(0xFF16181A), Color(0xFF5F6567), Color(0xFF767B7D), Color(0xFFB45834),
        Color(0xFF4F7A22), Color(0xFFA86A12), Color(0xFFC23B33),
        Color(0xFFFFFFFF), Color(0x1416181A), Color(0xFFFFFFFF), Color(0xFFF4C3AC), Color(0xFF16181A),
        Color(0xFFDCE8C4), Color(0xFF9DBF6A), Color(0xFF5E8A2E), Color(0x339DBF6A), Color(0xFF16181A),
        Color(0xFF25506A), Color(0xFF6FA3BC), Color(0xFF8A6FB3), Color(0xFFE07A52),
        soft = true, lightGround = true,
        tints = listOf(Color(0xFFD6E7EF), Color(0xFFDCE8C4), Color(0xFFE3DAEE), Color(0xFFF6DCCF)),
        tintInk = listOf(Color(0xFF3F5560), Color(0xFF4A5A33), Color(0xFF54466A), Color(0xFF6A4636))),
    ThemeTokens(ThemeId.TEAL, Color(0xFF0E2522), Color(0xFF081413), Color(0xFF050909),
        Color(0xFFE9EEED), Color(0xFF8FA3A0), Color(0xFF5F7471), Color(0xFF2DD4BF),
        Color(0xFF2DD4BF), Color(0xFFF5B041), Color(0xFFF0616D),
        Color(0x12FFFFFF), Color(0x14FFFFFF), Color(0x0FFFFFFF), Color(0x1F2DD4BF), Color(0xB8101616),
        Color(0xFF7AF5D0), Color(0xFF1FB89A), Color(0xFF0B5E57), Color(0x472DD4BF), Color.White,
        Color(0xFF0E7A6C), Color(0xFF1FB89A), Color(0xFF5BE7C4), Color(0xFF9FB7B3)),
    // Graphite keeps a single cool accent so stage and chart colours still separate on a grey base.
    ThemeTokens(ThemeId.GRAPHITE, Color(0xFF121212), Color(0xFF0B0B0B), Color(0xFF050505),
        Color(0xFFF2F2F2), Color(0xFF8A8A8A), Color(0xFF525252), Color(0xFFA7E3D0),
        Color(0xFF8FD3C0), Color(0xFFE2B96A), Color(0xFFFF6B6B),
        Color(0x12FFFFFF), Color(0x14FFFFFF), Color(0x0FFFFFFF), Color(0x26A7E3D0), Color(0xBF141414),
        Color(0xFFEDEDED), Color(0xFF9AA6A2), Color(0xFF34403C), Color(0x33A7E3D0), Color(0xFF0A0A0A),
        Color(0xFF6E7FA8), Color(0xFF9AA6C4), Color(0xFFC9B3E8), Color(0xFFE2B96A)),
).associateBy { it.id }

// Soft at night: the same layout on charcoal, deep muted tints with pale ink, and an inverted (light) dock.
// Not in THEMES: it is what Soft resolves to when the system is in dark mode.
val SOFT_DARK = ThemeTokens(ThemeId.SOFT, Color(0xFF131517), Color(0xFF131517), Color(0xFF131517),
    Color(0xFFEEF0EC), Color(0xFFA3A9A8), Color(0xFF858B8A), Color(0xFFF4B597),
    Color(0xFF9DC66C), Color(0xFFE8B26A), Color(0xFFEF7B6B),
    Color(0xFF1D2022), Color(0x14FFFFFF), Color(0xFF26292B), Color(0xFF4A3329), Color(0xFFE9ECE8),
    Color(0xFF3E4C33), Color(0xFF6F8F4A), Color(0xFF2A3622), Color(0x339DC66C), Color(0xFFEEF0EC),
    Color(0xFF4F86A6), Color(0xFF8FC0D6), Color(0xFFB09AD6), Color(0xFFF09A75),
    soft = true,
    tints = listOf(Color(0xFF22343D), Color(0xFF2A3424), Color(0xFF2F2939), Color(0xFF3B2B24)),
    tintInk = listOf(Color(0xFFBFD9E5), Color(0xFFCFE0B0), Color(0xFFD9CCEB), Color(0xFFF2CDBB)))

fun resolveTheme(id: ThemeId, systemDark: Boolean): ThemeTokens = if (id == ThemeId.SOFT && systemDark) SOFT_DARK else THEMES.getValue(id)

val LocalTheme = staticCompositionLocalOf { THEMES.getValue(ThemeId.TEAL) }

enum class Tint { SKY, SAGE, LAVENDER, PEACH }
