package app.fitdash.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun lineIcon(name: String, vararg strokes: String, fills: List<String> = emptyList()): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        strokes.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        fills.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.White)) }
    }.build()

object NavIcons {
    val today = lineIcon(
        "today",
        "M12 5a7 7 0 1 0 0 14a7 7 0 1 0 0 -14z",
        fills = listOf("M12 9.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0 -5z"),
    )
    val sleep = lineIcon("sleep", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")
    val activity = lineIcon("activity", "M3 12h4l3-7 4 14 3-7h4")
    val trends = lineIcon("trends", "M4 19V5M4 19h16M8 15l4-4 3 3 5-6")
    val bed = lineIcon("bed", "M3 18V7M3 14h18v4M21 14v-2a3 3 0 0 0-3-3h-7v5M7 11.5a1.5 1.5 0 1 0 0-.01")
    val walk = lineIcon("walk", "M13 3a1.5 1.5 0 1 0 0 3a1.5 1.5 0 1 0 0 -3z", "m9 21 3-7 3 3v4M7 11l3-3 4 1 3 3")
    val bolt = lineIcon("bolt", "M13 3 5 13h6l-1 8 8-10h-6z")
    val cup = lineIcon("cup", "M5 9h12v5a5 5 0 0 1-5 5h-2a5 5 0 0 1-5-5zM17 10h1.5a2.5 2.5 0 0 1 0 5H17M9 3v2M13 3v2")
    val moonSmall = lineIcon("moonSmall", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")
    val clock = lineIcon("clock", "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0 -16z", "M12 8v4l3 2")
    val heart = lineIcon("heart", "M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.5-7 10-7 10z")
    val share = lineIcon("share", "M12 15V4M8 8l4-4 4 4M5 13v6h14v-6")
    val sparkle = lineIcon("sparkle", "M12 4l1.8 5.2L19 11l-5.2 1.8L12 18l-1.8-5.2L5 11l5.2-1.8z")
    val back = lineIcon("back", "m14 6-6 6 6 6")
    val menu = lineIcon("menu", "M5 8h14M5 12h14M5 16h14")
    val drop = lineIcon("drop", "M12 3.5C9 7.5 6.5 10.5 6.5 14a5.5 5.5 0 0 0 11 0c0-3.5-2.5-6.5-5.5-10.5z")
    val info = lineIcon("info", "M12 11v6M12 7.5v.01")
}
