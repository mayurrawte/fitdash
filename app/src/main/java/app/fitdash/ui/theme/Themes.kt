package app.fitdash.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ThemeTokens(
    val bgTop: Color, val bgMid: Color, val bgBottom: Color,
    val text: Color, val muted: Color, val faint: Color, val accent: Color,
    val good: Color, val warn: Color, val bad: Color,
    val card: Color, val hairline: Color, val chip: Color, val chipStrong: Color, val navBg: Color,
    val orb1: Color, val orb2: Color, val orb3: Color, val glow: Color, val onOrb: Color,
    val deep: Color, val light: Color, val rem: Color, val awake: Color,
    // Tile tints in the order of Tint, with a dark ink of each for text on them.
    val tints: List<Color>, val tintInk: List<Color>,
)

val LOOK = ThemeTokens(Color(0xFFF3EEF8), Color(0xFFF4F3EF), Color(0xFFF4F3EF),
    Color(0xFF1E1B2E), Color(0xFF8A8398), Color(0xFFA9A3B5), Color(0xFF3B2357),
    Color(0xFF4F7A22), Color(0xFFA86A12), Color(0xFFC23B33),
    Color(0xFFFFFFFF), Color(0x141E1B2E), Color(0xB0FFFFFF), Color(0x1F3B2357), Color(0xD9FFFFFF),
    Color(0xFFE6DAFB), Color(0xFFA17DF0), Color(0xFF8A66E6), Color(0x33A17DF0), Color(0xFF1E1B2E),
    Color(0xFF7A5CFF), Color(0xFFB79CF0), Color(0xFF58C7FF), Color(0xFFFFB86B),
    tints = listOf(Color(0xFFD7E9FB), Color(0xFFF4DCE6), Color(0xFFFCEBD8), Color(0xFFEFE6D2), Color(0xFFDDEEFF)),
    tintInk = listOf(Color(0xFF285A99), Color(0xFF7A3D5C), Color(0xFF7A4A24), Color(0xFF5E5236), Color(0xFF163B6B)))

val LocalTheme = staticCompositionLocalOf { LOOK }

enum class Tint { SKY, ROSE, PEACH, SAND, WATER }
