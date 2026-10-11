package app.fitdash.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeId(val label: String) { TEAL("Midnight Teal"), PASTEL("Pastel"), TECHNO("Techno"), GRAPHITE("Graphite"), SUNRISE("Sunrise") }

data class ThemeTokens(
    val id: ThemeId,
    val bgTop: Color, val bgMid: Color, val bgBottom: Color,
    val text: Color, val muted: Color, val faint: Color, val accent: Color,
    val good: Color, val warn: Color, val bad: Color,
    val card: Color, val hairline: Color, val chip: Color, val chipStrong: Color, val navBg: Color,
    val orb1: Color, val orb2: Color, val orb3: Color, val glow: Color, val onOrb: Color,
    val deep: Color, val light: Color, val rem: Color, val awake: Color,
    val lightSystemBars: Boolean, val grid: Boolean, val monoEverywhere: Boolean,
)

val THEMES: Map<ThemeId, ThemeTokens> = listOf(
    ThemeTokens(ThemeId.TEAL, Color(0xFF0E2522), Color(0xFF081413), Color(0xFF050909),
        Color(0xFFE9EEED), Color(0xFF8FA3A0), Color(0xFF5F7471), Color(0xFF2DD4BF),
        Color(0xFF2DD4BF), Color(0xFFF5B041), Color(0xFFF0616D),
        Color(0x0BFFFFFF), Color(0x14FFFFFF), Color(0x0FFFFFFF), Color(0x1F2DD4BF), Color(0xB8101616),
        Color(0xFF7AF5D0), Color(0xFF1FB89A), Color(0xFF0B5E57), Color(0x472DD4BF), Color.White,
        Color(0xFF0E7A6C), Color(0xFF1FB89A), Color(0xFF5BE7C4), Color(0xFF9FB7B3),
        lightSystemBars = false, grid = false, monoEverywhere = false),
    ThemeTokens(ThemeId.PASTEL, Color(0xFFFBF4EE), Color(0xFFF3EAF6), Color(0xFFEAF2F3),
        Color(0xFF2B2633), Color(0xFF8A8396), Color(0xFFB3ADBD), Color(0xFF8F7BEA),
        Color(0xFF5DBB9A), Color(0xFFE8B15C), Color(0xFFE58A8A),
        Color(0xB3FFFFFF), Color(0x142B2633), Color(0x0F2B2633), Color(0x248F7BEA), Color(0xB3FFFFFF),
        Color(0xFFFFD9C7), Color(0xFFC9B8FF), Color(0xFFA8E6D3), Color(0x8CC9B8FF), Color(0xFF3A2F4A),
        Color(0xFF7C6BD6), Color(0xFFB8ABF2), Color(0xFF8FD9C2), Color(0xFFF2C4A8),
        lightSystemBars = true, grid = false, monoEverywhere = false),
    ThemeTokens(ThemeId.TECHNO, Color(0xFF020204), Color(0xFF020204), Color(0xFF020204),
        Color(0xFFE8F7FF), Color(0xFF5F7A88), Color(0xFF3A4A55), Color(0xFF00F0FF),
        Color(0xFF00F0FF), Color(0xFFFFE14D), Color(0xFFFF2BD6),
        Color(0x0900F0FF), Color(0x2E00F0FF), Color(0x1200F0FF), Color(0x1F00F0FF), Color(0xBF020204),
        Color(0xFF00F0FF), Color(0xFF7A2CFF), Color(0xFFFF2BD6), Color(0x5900F0FF), Color.White,
        Color(0xFF7A2CFF), Color(0xFF00A3B4), Color(0xFF00F0FF), Color(0xFFFF2BD6),
        lightSystemBars = false, grid = true, monoEverywhere = true),
    ThemeTokens(ThemeId.GRAPHITE, Color(0xFF121212), Color(0xFF0B0B0B), Color(0xFF050505),
        Color(0xFFF2F2F2), Color(0xFF7C7C7C), Color(0xFF4A4A4A), Color.White,
        Color(0xFFD9D9D9), Color(0xFFA6A6A6), Color(0xFFFF6B6B),
        Color(0x0AFFFFFF), Color(0x14FFFFFF), Color(0x0FFFFFFF), Color(0x1AFFFFFF), Color(0xBF141414),
        Color.White, Color(0xFF9A9A9A), Color(0xFF3A3A3A), Color(0x2EFFFFFF), Color(0xFF0A0A0A),
        Color(0xFF5A5A5A), Color(0xFF9A9A9A), Color(0xFFE0E0E0), Color(0xFFFF6B6B),
        lightSystemBars = false, grid = false, monoEverywhere = false),
    ThemeTokens(ThemeId.SUNRISE, Color(0xFF2A140C), Color(0xFF140A07), Color(0xFF080404),
        Color(0xFFFBEFE7), Color(0xFFB08F80), Color(0xFF6E5145), Color(0xFFFFB547),
        Color(0xFFFFB547), Color(0xFFFF8A4C), Color(0xFFFF5E5B),
        Color(0x0AFFFFFF), Color(0x12FFFFFF), Color(0x14FFB547), Color(0x24FFB547), Color(0xBF1A0E0A),
        Color(0xFFFFE0A3), Color(0xFFFF9A4C), Color(0xFFC2362E), Color(0x59FF9A4C), Color(0xFF2A0F05),
        Color(0xFFC2362E), Color(0xFFFF9A4C), Color(0xFFFFE0A3), Color(0xFF9E7B6E),
        lightSystemBars = false, grid = false, monoEverywhere = false),
).associateBy { it.id }

val LocalTheme = staticCompositionLocalOf { THEMES.getValue(ThemeId.TEAL) }
