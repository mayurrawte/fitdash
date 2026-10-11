package app.fitdash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object Palette {
    val Background = Color(0xFF0A0A0D)
    val Card = Color(0xFF15151B)
    val Divider = Color(0xFF22222B)
    val Text = Color(0xFFF2F2F2)
    val Muted = Color(0xFF8A8A93)
    val Faint = Color(0xFF5C5C66)
    val Calibrating = Color(0xFF5A5A66)
    val Green = Color(0xFF3DDC97)
    val DimGreen = Color(0xFF2E6B55)
    val Hrv = Color(0xFF4FD1C5)
    val Awake = Color(0xFFF5B041)
    val Rem = Color(0xFF8B7CFF)
    val Light = Color(0xFF5B6BD6)
    val Deep = Color(0xFF3A3F9E)
    val Amber = Color(0xFFF5B041)
    val Glass = Color(0x731E1E26)
}

object Type {
    val hero = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
    val value = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
    val label = TextStyle(fontSize = 10.sp, letterSpacing = 0.18.em)
    val body = TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
    val small = TextStyle(fontSize = 12.sp)
    val title = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
    val tile = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum")
    val tiny = TextStyle(fontSize = 10.sp)
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
fun FitdashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Green,
            background = Palette.Background,
            surface = Palette.Card,
            onBackground = Palette.Text,
            onSurface = Palette.Text,
        ),
        content = content,
    )
}
